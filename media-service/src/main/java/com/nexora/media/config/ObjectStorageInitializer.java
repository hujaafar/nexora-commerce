/*
 * File purpose: Creates and configures object storage initializer.
 */
package com.nexora.media.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

// Learning annotation: @Component marks the class for component scanning so Spring creates and manages one instance.
@Component
public class ObjectStorageInitializer implements ApplicationRunner {

    private final S3Client s3Client;
    private final String bucket;

    public ObjectStorageInitializer(
            S3Client s3Client,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.storage.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    // Learning annotation: @Override asks the Java compiler to verify that this method implements or overrides a parent contract.
    @Override
    public void run(ApplicationArguments args) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception exception) {
            if (exception.statusCode() != 404) {
                throw exception;
            }
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        }
    }
}
