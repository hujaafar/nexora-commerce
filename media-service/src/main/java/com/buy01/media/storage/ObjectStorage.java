package com.buy01.media.storage;

public interface ObjectStorage {

    void put(String objectKey, String contentType, byte[] content);

    byte[] get(String objectKey);

    void delete(String objectKey);
}
