package com.habittracker.groups.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRepository {

    /** Saves the group and synchronises its member list to match. */
    Group save(Group group);

    Optional<Group> findById(UUID id);

    List<Group> findByMember(UUID userId);

    /** All circles — used by the weekly digest, which visits every circle. */
    List<Group> findAll();

    /** Idempotent. */
    void shareHabit(UUID groupId, UUID habitId, UUID sharedBy);

    void unshareHabit(UUID groupId, UUID habitId);

    /** Removes every habit this user shared with the circle (when they leave it). */
    void unshareAllBy(UUID groupId, UUID userId);

    List<UUID> findSharedHabitIds(UUID groupId);

    /** Whether the habit is shared with any circle that {@code userId} belongs to. */
    boolean isSharedWithCircleOf(UUID habitId, UUID userId);
}
