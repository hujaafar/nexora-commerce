/* BUY-01 learning header
 * File purpose: Models the user account domain concept persisted or used by the service.
 * Learning focus: Domain modeling, MongoDB documents, indexes, and explicit enums.
 */
package com.buy01.user.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

// Learning annotation: @Document maps this class to the named MongoDB collection.
@Document(collection = "users")
public class UserAccount {

    // Learning annotation: @Id marks this field as the MongoDB document identifier.
    @Id
    private String id;

    private String name;

    // Learning annotation: @Indexed asks MongoDB to index this field for faster lookup or uniqueness enforcement.
    @Indexed(unique = true)
    private String email;

    private String passwordHash;
    private Role role;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;

    protected UserAccount() {
    }

    public UserAccount(
            String name,
            String email,
            String passwordHash,
            Role role,
            Instant now) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateProfile(String name, String avatarUrl, Instant now) {
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
