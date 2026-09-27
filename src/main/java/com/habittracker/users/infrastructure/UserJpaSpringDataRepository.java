package com.habittracker.users.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface UserJpaSpringDataRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByGoogleSubject(String googleSubject);

    @Modifying
    @Query(nativeQuery = true, value = """
        INSERT INTO users (id, google_subject, email, display_name, created_at)
        VALUES (:id, :googleSubject, :email, :displayName, :createdAt)
        ON CONFLICT (google_subject) DO NOTHING
        """)
    int insertIfAbsent(UUID id, String googleSubject, String email, String displayName, Instant createdAt);
}
