package com.habittracker.groups.application;

import com.habittracker.checkins.application.CheckInHistoryService;
import com.habittracker.checkins.domain.CheckIn;
import com.habittracker.common.domain.OwnerType;
import com.habittracker.groups.domain.Group;
import com.habittracker.groups.domain.GroupRepository;
import com.habittracker.groups.domain.Member;
import com.habittracker.habits.application.HabitService;
import com.habittracker.habits.domain.Habit;
import com.habittracker.streaks.application.HabitProgressService;
import com.habittracker.streaks.domain.HabitProgress;
import com.habittracker.streaks.domain.ProgressCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Cross-module workflow: combines circle membership (groups), habit
 * definitions (habits), check-in history (checkins) and streak rules
 * (streaks) into one view. Each module's rules stay in that module.
 */
@Service
public class GroupProgressService {

    static final int RECENT_ACTIVITY_LIMIT = 20;
    static final String FORMER_MEMBER = "Former member";

    private final GroupService groupService;
    private final GroupRepository groupRepository;
    private final HabitService habitService;
    private final HabitProgressService habitProgressService;
    private final CheckInHistoryService historyService;

    public GroupProgressService(GroupService groupService, GroupRepository groupRepository,
                                HabitService habitService, HabitProgressService habitProgressService,
                                CheckInHistoryService historyService) {
        this.groupService = groupService;
        this.groupRepository = groupRepository;
        this.habitService = habitService;
        this.habitProgressService = habitProgressService;
        this.historyService = historyService;
    }

    @Transactional(readOnly = true)
    public GroupProgress progress(UUID groupId, UUID viewerId, LocalDate today) {
        return progress(groupService.getGroup(groupId, viewerId), today);
    }

    /** For callers that have already authorized access (e.g. the weekly digest). */
    @Transactional(readOnly = true)
    public GroupProgress progress(Group group, LocalDate today) {
        LocalDate weekStart = ProgressCalculator.weekStartOf(today);
        Function<UUID, String> nameOf = userId -> group.member(userId).map(Member::displayName).orElse(FORMER_MEMBER);

        List<GroupProgress.SharedHabitProgress> shared = groupRepository.findSharedHabitIds(group.id()).stream()
            .map(habitService::getHabit)
            .filter(habit -> !habit.isArchived() && group.isMember(habit.ownerId()))
            .map(habit -> new GroupProgress.SharedHabitProgress(
                habit, habit.ownerId(), nameOf.apply(habit.ownerId()), habitProgressService.progress(habit, today)))
            .toList();

        List<GroupProgress.JointHabitProgress> joint = habitService.activeHabitsOf(OwnerType.GROUP, group.id()).stream()
            .map(habit -> new GroupProgress.JointHabitProgress(
                habit, habitProgressService.progress(habit, today), coverage(habit, weekStart, today, nameOf)))
            .toList();

        List<HabitProgress> all = Stream.concat(
            shared.stream().map(GroupProgress.SharedHabitProgress::progress),
            joint.stream().map(GroupProgress.JointHabitProgress::progress)).toList();
        int totalDone = all.stream().mapToInt(p -> p.thisWeek().done()).sum();
        int totalTarget = all.stream().mapToInt(p -> p.thisWeek().target()).sum();

        List<Habit> habits = new ArrayList<>();
        shared.forEach(s -> habits.add(s.habit()));
        joint.forEach(j -> habits.add(j.habit()));
        Map<UUID, Habit> habitsById = habits.stream().collect(Collectors.toMap(Habit::id, h -> h));
        List<GroupProgress.Activity> activity = historyService.recent(habits, RECENT_ACTIVITY_LIMIT).stream()
            .map(c -> new GroupProgress.Activity(c, habitsById.get(c.habitId()), nameOf.apply(c.performedByUserId())))
            .toList();

        return new GroupProgress(group, weekStart, totalDone, totalTarget, shared, joint, activity);
    }

    private List<GroupProgress.Coverage> coverage(Habit habit, LocalDate weekStart, LocalDate today,
                                                  Function<UUID, String> nameOf) {
        Map<UUID, Long> counts = historyService.checkInsBetween(habit, weekStart, today).stream()
            .collect(Collectors.groupingBy(CheckIn::performedByUserId, LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
            .map(e -> new GroupProgress.Coverage(e.getKey(), nameOf.apply(e.getKey()), e.getValue().intValue()))
            .sorted(Comparator.comparingInt(GroupProgress.Coverage::count).reversed())
            .toList();
    }
}
