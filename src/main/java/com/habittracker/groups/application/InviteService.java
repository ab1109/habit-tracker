package com.habittracker.groups.application;

import com.habittracker.common.domain.NotFoundException;
import com.habittracker.groups.domain.Group;
import com.habittracker.groups.domain.GroupInvite;
import com.habittracker.groups.domain.GroupInviteRepository;
import com.habittracker.groups.domain.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class InviteService {

    static final String INVALID = "This invite link is invalid or has expired";

    private final GroupService groupService;
    private final GroupRepository groupRepository;
    private final GroupInviteRepository inviteRepository;
    private final Clock clock;

    public InviteService(GroupService groupService, GroupRepository groupRepository,
                         GroupInviteRepository inviteRepository, Clock clock) {
        this.groupService = groupService;
        this.groupRepository = groupRepository;
        this.inviteRepository = inviteRepository;
        this.clock = clock;
    }

    public record InviteDetails(GroupInvite invite, Group group) {
    }

    /** Any member may create a link. */
    @Transactional
    public GroupInvite createInvite(UUID groupId, UUID requestedBy) {
        groupService.getGroup(groupId, requestedBy);
        return inviteRepository.save(GroupInvite.create(groupId, requestedBy, clock.instant()));
    }

    /** What the link leads to, so the joiner can see where they're going first. */
    @Transactional(readOnly = true)
    public InviteDetails details(String token) {
        GroupInvite invite = validInvite(token);
        Group group = groupRepository.findById(invite.groupId()).orElseThrow(() -> new NotFoundException(INVALID));
        return new InviteDetails(invite, group);
    }

    /** Idempotent: accepting when already a member just returns the circle. */
    @Transactional
    public Group accept(String token, UUID userId, String displayName) {
        Group group = details(token).group();
        return groupRepository.save(group.join(userId, displayName));
    }

    private GroupInvite validInvite(String token) {
        return inviteRepository.findByToken(token)
            .filter(invite -> !invite.isExpired(clock.instant()))
            .orElseThrow(() -> new NotFoundException(INVALID));
    }
}
