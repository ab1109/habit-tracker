package com.habittracker.habits.application;

import java.util.UUID;

/**
 * What habit access rules need to know about circles (joint and shared
 * habits). Declared in the habits module and implemented by the groups
 * module, so habits never depends on groups — every other module already
 * depends on habits.
 */
public interface CircleMembership {

    boolean isMember(UUID groupId, UUID userId);

    /** Whether the habit is shared with any circle that {@code userId} belongs to. */
    boolean isSharedWithCircleOf(UUID habitId, UUID userId);
}
