package com.habittracker.groups.application;

import com.habittracker.habits.application.CircleMembership;
import com.habittracker.groups.domain.Group;
import com.habittracker.groups.domain.GroupRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Answers the check-ins module's membership questions from the groups module's data. */
@Component
class CircleMembershipAdapter implements CircleMembership {

    private final GroupRepository groupRepository;

    CircleMembershipAdapter(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Override
    public boolean isMember(UUID groupId, UUID userId) {
        return groupRepository.findById(groupId).map(g -> g.isMember(userId)).orElse(false);
    }

    @Override
    public boolean isSharedWithCircleOf(UUID habitId, UUID userId) {
        return groupRepository.isSharedWithCircleOf(habitId, userId);
    }
}
