package com.habittracker.groups.domain;

import com.habittracker.common.domain.DomainValidationException;

import java.time.Instant;
import java.util.UUID;

/**
 * @param displayName how this person appears inside this circle — there is
 *                    no user profile yet, so each circle stores its own
 */
public record Member(UUID userId, String displayName, Instant joinedAt) {

    static final int MAX_DISPLAY_NAME_LENGTH = 60;

    public Member {
        if (displayName == null || displayName.isBlank()) {
            throw new DomainValidationException("displayName must not be blank");
        }
        displayName = displayName.strip();
        if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new DomainValidationException(
                "displayName must be at most " + MAX_DISPLAY_NAME_LENGTH + " characters");
        }
    }
}
