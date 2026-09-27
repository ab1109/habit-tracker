package com.habittracker.checkins.application;

import java.util.UUID;

/**
 * What the check-ins module needs to know about circles to authorize access
 * to joint and shared habits. Declared here and implemented by the groups
 * module, so check-ins never depends on groups (groups already depends on
 * check-ins for progress).
 */
public interface CircleMembership {

    boolean isMember(UUID groupId, UUID userId);

    /** Whether the habit is shared with any circle that {@code userId} belongs to. */
    boolean isSharedWithCircleOf(UUID habitId, UUID userId);
}
