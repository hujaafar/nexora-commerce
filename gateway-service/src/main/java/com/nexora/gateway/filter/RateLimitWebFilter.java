/*
 * File purpose: Limits authentication and media-write request bursts per client.
 */
package com.nexora.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

// Learning annotation: @Component marks the class for component scanning so Spring creates and manages one instance.
@Component
// Learning annotation: @Order sets this filter/component’s execution priority relative to other ordered components.
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitWebFilter implements WebFilter {

    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Limit authLimit;
    private final Limit mediaLimit;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();

    public RateLimitWebFilter(
            ObjectMapper objectMapper,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.rate-limit.auth.requests:20}") int authRequests,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.rate-limit.auth.window:60s}") Duration authWindow,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.rate-limit.media.requests:60}") int mediaRequests,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.rate-limit.media.window:60s}") Duration mediaWindow) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.authLimit = new Limit(authRequests, authWindow);
        this.mediaLimit = new Limit(mediaRequests, mediaWindow);
    }

    // Learning annotation: @Override asks the Java compiler to verify that this method implements or overrides a parent contract.
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        LimitSelection selection = selectLimit(exchange);
        if (!enabled || selection == null) {
            return chain.filter(exchange);
        }

        cleanupStaleBuckets();
        String bucketKey = selection.name() + ":" + clientAddress(exchange);
        TokenBucket bucket = buckets.computeIfAbsent(
                bucketKey,
                ignored -> new TokenBucket(selection.limit()));
        Consumption consumption = bucket.consume(selection.limit());

        exchange.getResponse().getHeaders().set(
                "X-RateLimit-Limit",
                String.valueOf(selection.limit().requests()));
        exchange.getResponse().getHeaders().set(
                "X-RateLimit-Remaining",
                String.valueOf(consumption.remaining()));

        if (consumption.allowed()) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(
                HttpHeaders.RETRY_AFTER,
                String.valueOf(consumption.retryAfterSeconds()));
        byte[] body = errorBody(exchange, consumption.retryAfterSeconds());
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private LimitSelection selectLimit(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/auth/")) {
            return new LimitSelection("auth", authLimit);
        }
        if (path.startsWith("/media/")
                && exchange.getRequest().getMethod() != HttpMethod.GET
                && exchange.getRequest().getMethod() != HttpMethod.HEAD) {
            return new LimitSelection("media", mediaLimit);
        }
        return null;
    }

    private String clientAddress(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }
        return remoteAddress.getAddress().getHostAddress();
    }

    private byte[] errorBody(ServerWebExchange exchange, long retryAfterSeconds) {
        try {
            return objectMapper.writeValueAsBytes(Map.of(
                    "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                    "error", HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                    "message", "Too many requests. Try again in "
                            + retryAfterSeconds
                            + " seconds.",
                    "path", exchange.getRequest().getPath().value()));
        } catch (Exception serializationFailure) {
            return "{\"status\":429,\"error\":\"Too Many Requests\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    private void cleanupStaleBuckets() {
        long currentRequest = requestCounter.incrementAndGet();
        if (buckets.size() < MAX_TRACKED_CLIENTS || currentRequest % 256 != 0) {
            return;
        }
        long staleBefore = System.nanoTime() - Duration.ofMinutes(10).toNanos();
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccess() < staleBefore);
    }

    private record LimitSelection(String name, Limit limit) {
    }

    private record Limit(int requests, Duration window) {

        private Limit {
            if (requests < 1) {
                throw new IllegalArgumentException("Rate-limit requests must be positive");
            }
            if (window == null || window.isZero() || window.isNegative()) {
                throw new IllegalArgumentException("Rate-limit window must be positive");
            }
        }
    }

    private record Consumption(boolean allowed, int remaining, long retryAfterSeconds) {
    }

    private static final class TokenBucket {

        private double tokens;
        private long lastRefill;
        private long lastAccess;

        private TokenBucket(Limit limit) {
            this.tokens = limit.requests();
            this.lastRefill = System.nanoTime();
            this.lastAccess = lastRefill;
        }

        private synchronized Consumption consume(Limit limit) {
            long now = System.nanoTime();
            double refillPerNanosecond =
                    (double) limit.requests() / limit.window().toNanos();
            tokens = Math.min(
                    limit.requests(),
                    tokens + ((now - lastRefill) * refillPerNanosecond));
            lastRefill = now;
            lastAccess = now;

            if (tokens >= 1) {
                tokens -= 1;
                return new Consumption(true, (int) Math.floor(tokens), 0);
            }

            long retryNanos = (long) Math.ceil((1 - tokens) / refillPerNanosecond);
            long retrySeconds = Math.max(
                    1,
                    (long) Math.ceil(retryNanos / 1_000_000_000d));
            return new Consumption(false, 0, retrySeconds);
        }

        private synchronized long lastAccess() {
            return lastAccess;
        }
    }
}
