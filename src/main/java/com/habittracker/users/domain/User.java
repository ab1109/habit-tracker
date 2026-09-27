package com.habittracker.users.domain;

import java.time.Instant;
import java.util.UUID;

/** A signed-in account. {@code googleSubject} is Google's stable id for the person. */
public record User(UUID id, String googleSubject, String email, String displayName, Instant createdAt) {

    public static User fromGoogle(String googleSubject, String email, String displayName) {
        String name = displayName != null && !displayName.isBlank() ? displayName.strip()
            : email != null && email.contains("@") ? email.substring(0, email.indexOf('@'))
            : "Friend";
        return new User(UUID.randomUUID(), googleSubject, email, name, Instant.now());
    }
}
