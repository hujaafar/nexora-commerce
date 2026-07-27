/* BUY-01 learning header
 * File purpose: Defines the object-storage port used by the media domain.
 * Learning focus: Ports-and-adapters design and keeping object bytes outside MongoDB.
 */
package com.buy01.media.storage;

public interface ObjectStorage {

    void put(String objectKey, String contentType, byte[] content);

    byte[] get(String objectKey);

    void delete(String objectKey);
}
