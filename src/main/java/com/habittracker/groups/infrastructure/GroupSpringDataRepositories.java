package com.habittracker.groups.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface GroupJpaSpringDataRepository extends JpaRepository<GroupJpaEntity, UUID> {

    @Query("""
        select g from GroupJpaEntity g
        where g.id in (select m.groupId from GroupMemberJpaEntity m where m.userId = :userId)
        order by g.createdAt
        """)
    List<GroupJpaEntity> findByMember(UUID userId);
}

interface GroupMemberJpaSpringDataRepository
    extends JpaRepository<GroupMemberJpaEntity, GroupMemberJpaEntity.Key> {

    List<GroupMemberJpaEntity> findByGroupId(UUID groupId);

    List<GroupMemberJpaEntity> findByGroupIdIn(Collection<UUID> groupIds);
}

interface SharedHabitJpaSpringDataRepository
    extends JpaRepository<SharedHabitJpaEntity, SharedHabitJpaEntity.Key> {

    List<SharedHabitJpaEntity> findByGroupIdOrderBySharedAt(UUID groupId);

    @Modifying
    @Query("delete from SharedHabitJpaEntity s where s.groupId = :groupId and s.sharedByUserId = :userId")
    void deleteSharedBy(UUID groupId, UUID userId);

    @Query("""
        select count(s) > 0 from SharedHabitJpaEntity s
        where s.habitId = :habitId
          and exists (select m from GroupMemberJpaEntity m where m.groupId = s.groupId and m.userId = :userId)
        """)
    boolean isSharedWithCircleOf(UUID habitId, UUID userId);
}
