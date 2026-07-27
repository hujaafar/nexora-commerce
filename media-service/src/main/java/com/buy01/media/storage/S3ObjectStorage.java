/*
 * File purpose: Implements image-byte storage using the S3-compatible SDK.
 */
package com.buy01.media.storage;

import com.buy01.media.exception.ObjectStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

// Learning annotation: @Component marks the class for component scanning so Spring creates and manages one instance.
@Component
public class S3ObjectStorage implements ObjectStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3ObjectStorage(
            S3Client s3Client,
            // Learning annotation: @Value injects an external configuration property into this constructor parameter or bean.
            @Value("${app.storage.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    // Learning annotation: @Override asks the Java compiler to verify that this method implements or overrides a parent contract.
    @Override
    public void put(String objectKey, String contentType, byte[] content) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (S3Exception exception) {
            throw new ObjectStorageException("Could not store the image", exception);
        }
    }

    // Learning annotation: @Override asks the Java compiler to verify that this method implements or overrides a parent contract.
    @Override
    public byte[] get(String objectKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            return response.asByteArray();
        } catch (S3Exception exception) {
            throw new ObjectStorageException("Could not read the image", exception);
        }
    }

    // Learning annotation: @Override asks the Java compiler to verify that this method implements or overrides a parent contract.
    @Override
    public void delete(String objectKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(request);
        } catch (S3Exception exception) {
            throw new ObjectStorageException("Could not delete the image", exception);
        }
    }
}
