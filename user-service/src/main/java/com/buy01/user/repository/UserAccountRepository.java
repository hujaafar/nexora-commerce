/*
 * File purpose: Provides persistence queries for user account data.
 */
package com.buy01.user.repository;

import com.buy01.user.domain.UserAccount;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserAccountRepository extends MongoRepository<UserAccount, String> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
