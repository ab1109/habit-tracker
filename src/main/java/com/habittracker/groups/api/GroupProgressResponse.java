package com.habittracker.groups.api;

import com.habittracker.groups.application.GroupProgress;
import com.habittracker.habits.api.HabitResponse;
import com.habittracker.streaks.api.HabitProgressResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GroupProgressResponse(
    UUID groupId,
    String groupName,
    LocalDate weekStart,
    LocalDate weekEnd,
    int totalDone,
    int totalTarget,
    List<SharedHabit> sharedHabits,
    List<JointHabit> jointHabits,
    List<Activity> recentActivity
) {

    public record SharedHabit(HabitResponse habit, UUID ownerUserId, String ownerDisplayName,
                              HabitProgressResponse progress) {
    }

    public record JointHabit(HabitResponse habit, HabitProgressResponse progress, List<Coverage> coverage) {
    }

    public record Coverage(UUID userId, String displayName, int count) {
    }

    public record Activity(UUID checkInId, UUID habitId, String habitName, boolean joint,
                           UUID performedByUserId, String displayName, Instant recordedAt, LocalDate localDate) {
    }

    public static GroupProgressResponse from(GroupProgress p) {
        return new GroupProgressResponse(
            p.group().id(),
            p.group().name(),
            p.weekStart(),
            p.weekEnd(),
            p.totalDone(),
            p.totalTarget(),
            p.sharedHabits().stream().map(s -> new SharedHabit(
                HabitResponse.from(s.habit()), s.ownerUserId(), s.ownerDisplayName(),
                HabitProgressResponse.from(s.habit(), s.progress()))).toList(),
            p.jointHabits().stream().map(j -> new JointHabit(
                HabitResponse.from(j.habit()), HabitProgressResponse.from(j.habit(), j.progress()),
                j.coverage().stream().map(c -> new Coverage(c.userId(), c.displayName(), c.count())).toList()
            )).toList(),
            p.recentActivity().stream().map(a -> new Activity(
                a.checkIn().id(), a.habit().id(), a.habit().name(), a.checkIn().isJoint(),
                a.checkIn().performedByUserId(), a.displayName(), a.checkIn().recordedAt(), a.checkIn().localDate()
            )).toList()
        );
    }
}
