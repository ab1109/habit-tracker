package com.habittracker.groups.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_invites")
class GroupInviteJpaEntity {

    @Id
    private String token;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected GroupInviteJpaEntity() {
    }

    GroupInviteJpaEntity(String token, UUID groupId, UUID createdBy, Instant createdAt, Instant expiresAt) {
        this.token = token;
        this.groupId = groupId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    String getToken() { return token; }
    UUID getGroupId() { return groupId; }
    UUID getCreatedBy() { return createdBy; }
    Instant getCreatedAt() { return createdAt; }
    Instant getExpiresAt() { return expiresAt; }
}
