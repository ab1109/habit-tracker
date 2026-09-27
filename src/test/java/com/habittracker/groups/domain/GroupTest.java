package com.habittracker.groups.domain;

import com.habittracker.common.domain.DomainValidationException;
import com.habittracker.common.domain.ForbiddenException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupTest {

    private final UUID maya = UUID.randomUUID();
    private final UUID dev = UUID.randomUUID();
    private final UUID tara = UUID.randomUUID();

    @Test
    void theCreatorIsTheFirstMember() {
        Group group = Group.create("Flat 4B", maya, "Maya");

        assertThat(group.isMember(maya)).isTrue();
        assertThat(group.members()).hasSize(1);
        assertThat(group.createdBy()).isEqualTo(maya);
    }

    @Test
    void anyMemberCanAddSomeoneAndAddingTwiceIsANoOp() {
        Group group = Group.create("Flat 4B", maya, "Maya").addMember(maya, dev, "Dev");

        Group withTara = group.addMember(dev, tara, "Tara");

        assertThat(withTara.members()).extracting(Member::displayName).containsExactly("Maya", "Dev", "Tara");
        assertThat(withTara.addMember(maya, tara, "Tara again")).isSameAs(withTara);
    }

    @Test
    void aNonMemberCannotAddPeople() {
        Group group = Group.create("Flat 4B", maya, "Maya");

        assertThatThrownBy(() -> group.addMember(dev, tara, "Tara")).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void aMemberCanLeaveButCannotRemoveOthers() {
        Group group = Group.create("Flat 4B", maya, "Maya").addMember(maya, dev, "Dev").addMember(maya, tara, "Tara");

        assertThat(group.removeMember(dev, dev).isMember(dev)).isFalse();
        assertThatThrownBy(() -> group.removeMember(dev, tara)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void theCreatorCanRemoveAnyone() {
        Group group = Group.create("Flat 4B", maya, "Maya").addMember(maya, dev, "Dev");

        assertThat(group.removeMember(maya, dev).isMember(dev)).isFalse();
    }

    @Test
    void joiningThroughAnInviteNeedsNoExistingMembershipAndIsIdempotent() {
        Group group = Group.create("Flat 4B", maya, "Maya");

        Group joined = group.join(dev, "Dev");

        assertThat(joined.isMember(dev)).isTrue();
        assertThat(joined.join(dev, "Dev again")).isSameAs(joined);
    }

    @Test
    void anInviteExpiresAfterSevenDays() {
        Instant now = Instant.parse("2026-09-20T10:00:00Z");
        GroupInvite invite = GroupInvite.create(UUID.randomUUID(), maya, now);

        assertThat(invite.token()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(invite.isExpired(now.plus(Duration.ofDays(7)).minusSeconds(1))).isFalse();
        assertThat(invite.isExpired(now.plus(Duration.ofDays(7)))).isTrue();
        assertThat(GroupInvite.create(UUID.randomUUID(), maya, now).token()).isNotEqualTo(invite.token());
    }

    @Test
    void blankNamesAreRejected() {
        assertThatThrownBy(() -> Group.create(" ", maya, "Maya")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Group.create("Flat 4B", maya, "")).isInstanceOf(DomainValidationException.class);
    }
}
