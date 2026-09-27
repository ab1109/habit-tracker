package com.habittracker.groups.domain;

import com.habittracker.common.domain.DomainValidationException;
import com.habittracker.common.domain.ForbiddenException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A circle of people. Membership rules: any member may add someone; a
 * member may remove themselves; the creator may remove anyone. There are
 * no other roles.
 */
public record Group(UUID id, String name, UUID createdBy, Instant createdAt, List<Member> members) {

    public Group {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("name must not be blank");
        }
        name = name.strip();
        members = members.stream().sorted(Comparator.comparing(Member::joinedAt)).toList();
    }

    public static Group create(String name, UUID creatorId, String creatorDisplayName) {
        Instant now = Instant.now();
        return new Group(UUID.randomUUID(), name, creatorId, now,
            List.of(new Member(creatorId, creatorDisplayName, now)));
    }

    public boolean isMember(UUID userId) {
        return member(userId).isPresent();
    }

    public Optional<Member> member(UUID userId) {
        return members.stream().filter(m -> m.userId().equals(userId)).findFirst();
    }

    public void requireMember(UUID userId) {
        if (!isMember(userId)) {
            throw new ForbiddenException("You are not a member of this circle");
        }
    }

    /** Idempotent: adding someone who is already a member changes nothing. */
    public Group addMember(UUID requestedBy, UUID userId, String displayName) {
        requireMember(requestedBy);
        if (isMember(userId)) {
            return this;
        }
        List<Member> updated = new ArrayList<>(members);
        updated.add(new Member(userId, displayName, Instant.now()));
        return new Group(id, name, createdBy, createdAt, updated);
    }

    public Group removeMember(UUID requestedBy, UUID userId) {
        requireMember(requestedBy);
        if (!requestedBy.equals(userId) && !requestedBy.equals(createdBy)) {
            throw new ForbiddenException("Only the circle's creator can remove other members");
        }
        if (!isMember(userId)) {
            return this;
        }
        List<Member> updated = members.stream().filter(m -> !m.userId().equals(userId)).toList();
        return new Group(id, name, createdBy, createdAt, updated);
    }
}
