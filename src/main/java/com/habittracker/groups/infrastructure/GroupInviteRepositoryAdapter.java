package com.habittracker.groups.infrastructure;

import com.habittracker.groups.domain.GroupInvite;
import com.habittracker.groups.domain.GroupInviteRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

interface GroupInviteJpaSpringDataRepository extends JpaRepository<GroupInviteJpaEntity, String> {
}

@Repository
class GroupInviteRepositoryAdapter implements GroupInviteRepository {

    private final GroupInviteJpaSpringDataRepository springDataRepository;

    GroupInviteRepositoryAdapter(GroupInviteJpaSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public GroupInvite save(GroupInvite invite) {
        springDataRepository.save(new GroupInviteJpaEntity(
            invite.token(), invite.groupId(), invite.createdBy(), invite.createdAt(), invite.expiresAt()));
        return invite;
    }

    @Override
    public Optional<GroupInvite> findByToken(String token) {
        return springDataRepository.findById(token).map(e ->
            new GroupInvite(e.getToken(), e.getGroupId(), e.getCreatedBy(), e.getCreatedAt(), e.getExpiresAt()));
    }
}
