package com.habittracker.groups.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_members")
@IdClass(GroupMemberJpaEntity.Key.class)
class GroupMemberJpaEntity {

    record Key(UUID groupId, UUID userId) implements Serializable {
        Key() {
            this(null, null);
        }
    }

    @Id
    @Column(name = "group_id")
    private UUID groupId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected GroupMemberJpaEntity() {
    }

    GroupMemberJpaEntity(UUID groupId, UUID userId, String displayName, Instant joinedAt) {
        this.groupId = groupId;
        this.userId = userId;
        this.displayName = displayName;
        this.joinedAt = joinedAt;
    }

    UUID getGroupId() { return groupId; }
    UUID getUserId() { return userId; }
    String getDisplayName() { return displayName; }
    Instant getJoinedAt() { return joinedAt; }
}
