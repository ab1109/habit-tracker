package com.habittracker.notifications.application;

import com.habittracker.groups.application.GroupProgress;
import com.habittracker.notifications.domain.WeeklyDigest;
import com.habittracker.streaks.domain.WeekProgress;

import java.util.List;

/**
 * Turns a circle's derived progress into digest content. Reads only — the
 * digest never changes any source-of-truth data.
 */
final class WeeklyDigestComposer {

    private WeeklyDigestComposer() {
    }

    /**
     * A shared habit "held" when its current streak is still alive (the
     * in-progress period doesn't break it); otherwise it "dropped off". A
     * joint habit is listed when the circle is short of this week's target.
     */
    static WeeklyDigest compose(GroupProgress progress) {
        List<WeeklyDigest.OnTarget> onTarget = progress.sharedHabits().stream()
            .filter(s -> s.progress().currentStreak() > 0)
            .map(s -> new WeeklyDigest.OnTarget(s.ownerDisplayName(), s.habit().name()))
            .toList();

        List<WeeklyDigest.Behind> behind = progress.sharedHabits().stream()
            .filter(s -> s.progress().currentStreak() == 0)
            .map(s -> {
                WeekProgress week = s.progress().thisWeek();
                return new WeeklyDigest.Behind(s.ownerDisplayName(), s.habit().name(), week.done(), week.target());
            })
            .toList();

        List<WeeklyDigest.Uncovered> uncovered = progress.jointHabits().stream()
            .filter(j -> !j.progress().thisWeek().met())
            .map(j -> new WeeklyDigest.Uncovered(
                j.habit().name(), j.progress().thisWeek().target() - j.progress().thisWeek().done()))
            .toList();

        return new WeeklyDigest(progress.group().id(), progress.group().name(), progress.weekStart(),
            onTarget, behind, uncovered);
    }
}
