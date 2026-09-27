package com.habittracker.groups.api;

import com.habittracker.groups.domain.Group;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GroupResponse(UUID id, String name, UUID createdBy, Instant createdAt, List<MemberResponse> members) {

    public record MemberResponse(UUID userId, String displayName, Instant joinedAt) {
    }

    public static GroupResponse from(Group group) {
        return new GroupResponse(group.id(), group.name(), group.createdBy(), group.createdAt(),
            group.members().stream()
                .map(m -> new MemberResponse(m.userId(), m.displayName(), m.joinedAt()))
                .toList());
    }
}
