package com.habittracker.users.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
class UserJpaEntity {

    @Id
    private UUID id;

    @Column(name = "google_subject", nullable = false)
    private String googleSubject;

    @Column
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserJpaEntity() {
        // Read-only mapping: rows are inserted with INSERT ... ON CONFLICT DO NOTHING.
    }

    UUID getId() { return id; }
    String getGoogleSubject() { return googleSubject; }
    String getEmail() { return email; }
    String getDisplayName() { return displayName; }
    Instant getCreatedAt() { return createdAt; }
}
