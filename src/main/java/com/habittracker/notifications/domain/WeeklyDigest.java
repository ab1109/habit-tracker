package com.habittracker.notifications.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * One circle's week in summary: who held their streak, who dropped off, and
 * which joint habits went uncovered.
 */
public record WeeklyDigest(
    UUID groupId,
    String groupName,
    LocalDate weekStart,
    List<OnTarget> onTarget,
    List<Behind> behind,
    List<Uncovered> uncoveredJointHabits
) {

    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    public record OnTarget(String displayName, String habitName) {
    }

    public record Behind(String displayName, String habitName, int done, int target) {
    }

    /** @param uncoveredDays required check-ins this week that no member made */
    public record Uncovered(String habitName, int uncoveredDays) {
    }

    public LocalDate weekEnd() {
        return weekStart.plusDays(6);
    }

    public String subject() {
        return groupName + " — your week (" + weekStart.format(DAY_MONTH) + "–" + weekEnd().format(DAY_MONTH) + ")";
    }

    public String body() {
        StringBuilder body = new StringBuilder();
        if (onTarget.isEmpty() && behind.isEmpty() && uncoveredJointHabits.isEmpty()) {
            return "Nothing is shared with " + groupName + " yet.";
        }
        if (!onTarget.isEmpty()) {
            body.append("Held their streak:\n");
            onTarget.forEach(o -> body.append("  • ").append(o.displayName()).append(" — ").append(o.habitName()).append('\n'));
        }
        if (!behind.isEmpty()) {
            body.append("Dropped off:\n");
            behind.forEach(b -> body.append("  • ").append(b.displayName()).append(" — ").append(b.habitName())
                .append(" (").append(b.done()).append(" of ").append(b.target()).append(")\n"));
        }
        if (!uncoveredJointHabits.isEmpty()) {
            body.append("Went uncovered:\n");
            uncoveredJointHabits.forEach(u -> body.append("  • ").append(u.habitName()).append(" — ")
                .append(u.uncoveredDays()).append(u.uncoveredDays() == 1 ? " day" : " days").append('\n'));
        }
        return body.toString().stripTrailing();
    }
}
