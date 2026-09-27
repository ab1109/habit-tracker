package com.habittracker.groups.domain;

import java.util.Optional;

public interface GroupInviteRepository {

    GroupInvite save(GroupInvite invite);

    Optional<GroupInvite> findByToken(String token);
}
