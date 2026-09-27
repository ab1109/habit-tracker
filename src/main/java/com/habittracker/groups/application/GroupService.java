package com.habittracker.groups.application;

import com.habittracker.common.domain.DomainValidationException;
import com.habittracker.common.domain.ForbiddenException;
import com.habittracker.common.domain.NotFoundException;
import com.habittracker.common.domain.OwnerType;
import com.habittracker.groups.domain.Group;
import com.habittracker.groups.domain.GroupRepository;
import com.habittracker.habits.application.HabitService;
import com.habittracker.habits.domain.Habit;
import com.habittracker.habits.domain.Schedule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final HabitService habitService;

    public GroupService(GroupRepository groupRepository, HabitService habitService) {
        this.groupRepository = groupRepository;
        this.habitService = habitService;
    }

    @Transactional
    public Group createGroup(String name, UUID creatorId, String creatorDisplayName) {
        return groupRepository.save(Group.create(name, creatorId, creatorDisplayName));
    }

    @Transactional(readOnly = true)
    public List<Group> groupsOf(UUID userId) {
        return groupRepository.findByMember(userId);
    }

    @Transactional(readOnly = true)
    public Group getGroup(UUID groupId, UUID viewerId) {
        Group group = load(groupId);
        group.requireMember(viewerId);
        return group;
    }

    @Transactional
    public Group addMember(UUID groupId, UUID requestedBy, UUID userId, String displayName) {
        return groupRepository.save(load(groupId).addMember(requestedBy, userId, displayName));
    }

    /** A member who leaves takes the habits they shared with them. */
    @Transactional
    public void removeMember(UUID groupId, UUID requestedBy, UUID userId) {
        Group group = load(groupId).removeMember(requestedBy, userId);
        groupRepository.save(group);
        groupRepository.unshareAllBy(groupId, userId);
    }

    @Transactional
    public void shareHabit(UUID groupId, UUID requestedBy, UUID habitId) {
        getGroup(groupId, requestedBy);
        Habit habit = habitService.getHabit(habitId);
        if (habit.ownerType() != OwnerType.USER || !habit.ownerId().equals(requestedBy)) {
            throw new ForbiddenException("You can only share your own personal habits");
        }
        if (habit.isArchived()) {
            throw new DomainValidationException("An archived habit can't be shared");
        }
        groupRepository.shareHabit(groupId, habitId, requestedBy);
    }

    @Transactional
    public void unshareHabit(UUID groupId, UUID requestedBy, UUID habitId) {
        getGroup(groupId, requestedBy);
        Habit habit = habitService.getHabit(habitId);
        if (!habit.ownerId().equals(requestedBy)) {
            throw new ForbiddenException("Only the habit's owner can stop sharing it");
        }
        groupRepository.unshareHabit(groupId, habitId);
    }

    /** A joint habit is owned by the circle: one check-in by any member covers the day for everyone. */
    @Transactional
    public Habit createJointHabit(UUID groupId, UUID requestedBy, String name, Schedule schedule) {
        getGroup(groupId, requestedBy);
        return habitService.createHabit(OwnerType.GROUP, groupId, name, schedule);
    }

    private Group load(UUID groupId) {
        return groupRepository.findById(groupId)
            .orElseThrow(() -> new NotFoundException("Circle not found: " + groupId));
    }
}
