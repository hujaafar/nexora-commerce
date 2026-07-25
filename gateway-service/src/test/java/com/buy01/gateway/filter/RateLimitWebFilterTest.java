package com.buy01.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class RateLimitWebFilterTest {

    @Test
    void limitsAuthenticationRequestsPerClient() {
        RateLimitWebFilter filter = filter(2, 10);
        AtomicInteger accepted = new AtomicInteger();
        WebFilterChain chain = exchange -> {
            accepted.incrementAndGet();
            return Mono.empty();
        };

        MockServerWebExchange first = exchange(HttpMethod.POST, "/auth/login", "203.0.113.9");
        MockServerWebExchange second = exchange(HttpMethod.POST, "/auth/login", "203.0.113.9");
        MockServerWebExchange third = exchange(HttpMethod.POST, "/auth/login", "203.0.113.9");

        filter.filter(first, chain).block();
        filter.filter(second, chain).block();
        filter.filter(third, chain).block();

        assertThat(accepted).hasValue(2);
        assertThat(third.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(third.getResponse().getHeaders().getFirst("Retry-After"))
                .isNotBlank();
        assertThat(third.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"))
                .isEqualTo("0");
    }

    @Test
    void leavesPublicReadsOutsideTheLimiter() {
        RateLimitWebFilter filter = filter(1, 1);
        AtomicInteger accepted = new AtomicInteger();
        WebFilterChain chain = exchange -> {
            accepted.incrementAndGet();
            return Mono.empty();
        };

        for (int request = 0; request < 5; request++) {
            filter.filter(exchange(HttpMethod.GET, "/products", "203.0.113.10"), chain).block();
            filter.filter(
                    exchange(HttpMethod.GET, "/media/images/example", "203.0.113.10"),
                    chain).block();
        }

        assertThat(accepted).hasValue(10);
    }

    @Test
    void separatesMediaWritesFromAuthenticationTraffic() {
        RateLimitWebFilter filter = filter(1, 1);
        AtomicInteger accepted = new AtomicInteger();
        WebFilterChain chain = exchange -> {
            accepted.incrementAndGet();
            return Mono.empty();
        };

        MockServerWebExchange auth =
                exchange(HttpMethod.POST, "/auth/login", "203.0.113.11");
        MockServerWebExchange media =
                exchange(HttpMethod.POST, "/media/images", "203.0.113.11");

        filter.filter(auth, chain).block();
        filter.filter(media, chain).block();

        assertThat(accepted).hasValue(2);
        assertThat(auth.getResponse().getStatusCode()).isNull();
        assertThat(media.getResponse().getStatusCode()).isNull();
    }

    private RateLimitWebFilter filter(int authRequests, int mediaRequests) {
        return new RateLimitWebFilter(
                new ObjectMapper(),
                true,
                authRequests,
                Duration.ofMinutes(1),
                mediaRequests,
                Duration.ofMinutes(1));
    }

    private MockServerWebExchange exchange(
            HttpMethod method,
            String path,
            String clientAddress) {
        return MockServerWebExchange.from(MockServerHttpRequest
                .method(method, path)
                .header("X-Forwarded-For", clientAddress)
                .build());
    }
}
