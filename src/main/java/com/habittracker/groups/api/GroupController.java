package com.habittracker.groups.api;

import com.habittracker.common.api.CurrentUser;
import com.habittracker.checkins.domain.LocalDateResolver;
import com.habittracker.groups.application.GroupProgressService;
import com.habittracker.groups.application.GroupService;
import com.habittracker.groups.domain.Group;
import com.habittracker.habits.api.CreateHabitRequest;
import com.habittracker.habits.api.HabitResponse;
import com.habittracker.habits.domain.Habit;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;
    private final GroupProgressService progressService;
    private final Clock clock;

    public GroupController(GroupService groupService, GroupProgressService progressService, Clock clock) {
        this.groupService = groupService;
        this.progressService = progressService;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
        @CurrentUser UUID userId,
        @Valid @RequestBody GroupRequests.CreateGroup request
    ) {
        Group group = groupService.createGroup(request.name(), userId, request.displayName());
        return ResponseEntity.created(URI.create("/groups/" + group.id())).body(GroupResponse.from(group));
    }

    @GetMapping
    public List<GroupResponse> myGroups(@CurrentUser UUID userId) {
        return groupService.groupsOf(userId).stream().map(GroupResponse::from).toList();
    }

    @GetMapping("/{groupId}")
    public GroupResponse getGroup(@PathVariable UUID groupId, @CurrentUser UUID userId) {
        return GroupResponse.from(groupService.getGroup(groupId, userId));
    }

    @PostMapping("/{groupId}/members")
    public GroupResponse addMember(
        @PathVariable UUID groupId,
        @CurrentUser UUID userId,
        @Valid @RequestBody GroupRequests.AddMember request
    ) {
        return GroupResponse.from(groupService.addMember(groupId, userId, request.userId(), request.displayName()));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
        @PathVariable UUID groupId,
        @PathVariable UUID memberId,
        @CurrentUser UUID userId
    ) {
        groupService.removeMember(groupId, userId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/shared-habits")
    public ResponseEntity<Void> shareHabit(
        @PathVariable UUID groupId,
        @CurrentUser UUID userId,
        @Valid @RequestBody GroupRequests.ShareHabit request
    ) {
        groupService.shareHabit(groupId, userId, request.habitId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupId}/shared-habits/{habitId}")
    public ResponseEntity<Void> unshareHabit(
        @PathVariable UUID groupId,
        @PathVariable UUID habitId,
        @CurrentUser UUID userId
    ) {
        groupService.unshareHabit(groupId, userId, habitId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/habits")
    public ResponseEntity<HabitResponse> createJointHabit(
        @PathVariable UUID groupId,
        @CurrentUser UUID userId,
        @Valid @RequestBody CreateHabitRequest request
    ) {
        Habit habit = groupService.createJointHabit(groupId, userId, request.name(), request.toSchedule());
        return ResponseEntity.created(URI.create("/habits/" + habit.id())).body(HabitResponse.from(habit));
    }

    @GetMapping("/{groupId}/progress")
    public GroupProgressResponse progress(
        @PathVariable UUID groupId,
        @CurrentUser UUID userId,
        @RequestHeader("X-Timezone") String timezone
    ) {
        var today = LocalDateResolver.resolve(clock.instant(), ZoneId.of(timezone));
        return GroupProgressResponse.from(progressService.progress(groupId, userId, today));
    }
}
