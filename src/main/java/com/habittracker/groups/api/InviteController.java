package com.habittracker.groups.api;

import com.habittracker.common.api.CurrentUser;
import com.habittracker.groups.application.InviteService;
import com.habittracker.groups.domain.GroupInvite;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    public record InviteResponse(String token, UUID groupId, Instant expiresAt) {

        static InviteResponse from(GroupInvite invite) {
            return new InviteResponse(invite.token(), invite.groupId(), invite.expiresAt());
        }
    }

    public record InviteDetailsResponse(UUID groupId, String groupName, int memberCount, Instant expiresAt,
                                        boolean alreadyMember) {
    }

    public record AcceptInviteRequest(@NotBlank String displayName) {
    }

    @PostMapping("/groups/{groupId}/invites")
    public ResponseEntity<InviteResponse> createInvite(@PathVariable UUID groupId, @CurrentUser UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(InviteResponse.from(inviteService.createInvite(groupId, userId)));
    }

    @GetMapping("/invites/{token}")
    public InviteDetailsResponse details(@PathVariable String token, @CurrentUser UUID userId) {
        var details = inviteService.details(token);
        return new InviteDetailsResponse(details.group().id(), details.group().name(),
            details.group().members().size(), details.invite().expiresAt(), details.group().isMember(userId));
    }

    @PostMapping("/invites/{token}/accept")
    public GroupResponse accept(@PathVariable String token, @CurrentUser UUID userId,
                                @Valid @RequestBody AcceptInviteRequest request) {
        return GroupResponse.from(inviteService.accept(token, userId, request.displayName()));
    }
}
