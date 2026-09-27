package com.habittracker.groups.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "groups")
class GroupJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GroupJpaEntity() {
    }

    GroupJpaEntity(UUID id, String name, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    String getName() { return name; }
    UUID getCreatedBy() { return createdBy; }
    Instant getCreatedAt() { return createdAt; }
}
