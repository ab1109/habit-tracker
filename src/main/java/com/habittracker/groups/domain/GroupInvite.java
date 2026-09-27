package com.habittracker.groups.domain;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * A link anyone can use to join a circle until it expires. The token is the
 * only secret: 32 random bytes, so it can't be guessed.
 */
public record GroupInvite(String token, UUID groupId, UUID createdBy, Instant createdAt, Instant expiresAt) {

    public static final Duration VALID_FOR = Duration.ofDays(7);

    private static final SecureRandom RANDOM = new SecureRandom();

    public static GroupInvite create(UUID groupId, UUID createdBy, Instant now) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new GroupInvite(token, groupId, createdBy, now, now.plus(VALID_FOR));
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
