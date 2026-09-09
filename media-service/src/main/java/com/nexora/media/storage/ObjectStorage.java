/*
 * File purpose: Defines the object-storage port used by the media domain.
 */
package com.nexora.media.storage;

public interface ObjectStorage {

    void put(String objectKey, String contentType, byte[] content);

    byte[] get(String objectKey);

    void delete(String objectKey);
}
