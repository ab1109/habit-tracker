package com.habittracker.groups.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

final class GroupRequests {

    private GroupRequests() {
    }

    record CreateGroup(@NotBlank String name, @NotBlank String displayName) {
    }

    record AddMember(@NotNull UUID userId, @NotBlank String displayName) {
    }

    record ShareHabit(@NotNull UUID habitId) {
    }
}
