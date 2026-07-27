/*
 * File purpose: Provides persistence queries for media asset data.
 */
package com.buy01.media.repository;

import com.buy01.media.domain.MediaAsset;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MediaAssetRepository extends MongoRepository<MediaAsset, String> {

    List<MediaAsset> findAllBySellerIdOrderByCreatedAtDesc(String sellerId);

    List<MediaAsset> findAllByOrderByCreatedAtDesc();
}
