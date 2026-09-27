package com.habittracker.groups.infrastructure;

import com.habittracker.groups.domain.Group;
import com.habittracker.groups.domain.GroupRepository;
import com.habittracker.groups.domain.Member;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
class GroupRepositoryAdapter implements GroupRepository {

    private final GroupJpaSpringDataRepository groups;
    private final GroupMemberJpaSpringDataRepository members;
    private final SharedHabitJpaSpringDataRepository sharedHabits;

    GroupRepositoryAdapter(GroupJpaSpringDataRepository groups,
                           GroupMemberJpaSpringDataRepository members,
                           SharedHabitJpaSpringDataRepository sharedHabits) {
        this.groups = groups;
        this.members = members;
        this.sharedHabits = sharedHabits;
    }

    @Override
    public Group save(Group group) {
        groups.save(new GroupJpaEntity(group.id(), group.name(), group.createdBy(), group.createdAt()));

        Set<UUID> wanted = group.members().stream().map(Member::userId).collect(Collectors.toSet());
        List<GroupMemberJpaEntity> existing = members.findByGroupId(group.id());
        members.deleteAll(existing.stream().filter(m -> !wanted.contains(m.getUserId())).toList());
        Set<UUID> present = existing.stream().map(GroupMemberJpaEntity::getUserId).collect(Collectors.toSet());
        members.saveAll(group.members().stream()
            .filter(m -> !present.contains(m.userId()))
            .map(m -> new GroupMemberJpaEntity(group.id(), m.userId(), m.displayName(), m.joinedAt()))
            .toList());
        members.flush();

        return group;
    }

    @Override
    public Optional<Group> findById(UUID id) {
        return groups.findById(id).map(g -> toDomain(g, members.findByGroupId(id)));
    }

    @Override
    public List<Group> findByMember(UUID userId) {
        return withMembers(groups.findByMember(userId));
    }

    @Override
    public List<Group> findAll() {
        return withMembers(groups.findAll());
    }

    @Override
    public void shareHabit(UUID groupId, UUID habitId, UUID sharedBy) {
        if (!sharedHabits.existsById(new SharedHabitJpaEntity.Key(groupId, habitId))) {
            sharedHabits.save(new SharedHabitJpaEntity(groupId, habitId, sharedBy, Instant.now()));
        }
    }

    @Override
    public void unshareHabit(UUID groupId, UUID habitId) {
        sharedHabits.deleteById(new SharedHabitJpaEntity.Key(groupId, habitId));
    }

    @Override
    public void unshareAllBy(UUID groupId, UUID userId) {
        sharedHabits.deleteSharedBy(groupId, userId);
    }

    @Override
    public List<UUID> findSharedHabitIds(UUID groupId) {
        return sharedHabits.findByGroupIdOrderBySharedAt(groupId).stream()
            .map(SharedHabitJpaEntity::getHabitId).toList();
    }

    @Override
    public boolean isSharedWithCircleOf(UUID habitId, UUID userId) {
        return sharedHabits.isSharedWithCircleOf(habitId, userId);
    }

    private List<Group> withMembers(List<GroupJpaEntity> entities) {
        Map<UUID, List<GroupMemberJpaEntity>> byGroup = members
            .findByGroupIdIn(entities.stream().map(GroupJpaEntity::getId).toList()).stream()
            .collect(Collectors.groupingBy(GroupMemberJpaEntity::getGroupId));
        return entities.stream().map(g -> toDomain(g, byGroup.getOrDefault(g.getId(), List.of()))).toList();
    }

    private static Group toDomain(GroupJpaEntity entity, List<GroupMemberJpaEntity> memberEntities) {
        return new Group(entity.getId(), entity.getName(), entity.getCreatedBy(), entity.getCreatedAt(),
            memberEntities.stream()
                .map(m -> new Member(m.getUserId(), m.getDisplayName(), m.getJoinedAt()))
                .toList());
    }
}
