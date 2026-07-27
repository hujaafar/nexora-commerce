/*
 * File purpose: Creates and configures object storage config.
 */
package com.buy01.media.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

// Learning annotation: @Configuration marks this class as a source of Spring bean definitions and application setup.
@Configuration
public class ObjectStorageConfig {

    // Learning annotation: @Bean registers the returned object in Spring’s IoC container so other classes can inject it.
    @Bean
    S3Client s3Client(
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.storage.endpoint}") URI endpoint,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.storage.region}") String region,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.storage.access-key}") String accessKey,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.storage.secret-key}") String secretKey) {
        return S3Client.builder()
                .endpointOverride(endpoint)
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true)
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();
    }
}
