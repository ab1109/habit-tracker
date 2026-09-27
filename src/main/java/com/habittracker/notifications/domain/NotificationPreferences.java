package com.habittracker.notifications.domain;

import java.time.ZoneId;
import java.util.UUID;

/**
 * @param timezone when "Sunday evening" is for this user. Used only for
 *                 scheduling notifications; check-ins still resolve their
 *                 local date from the timezone sent with each request.
 */
public record NotificationPreferences(UUID userId, boolean weeklyDigestEnabled, ZoneId timezone) {

    /** What applies until the user saves preferences: digest on, UTC. */
    public static NotificationPreferences defaults(UUID userId) {
        return new NotificationPreferences(userId, true, ZoneId.of("UTC"));
    }
}
