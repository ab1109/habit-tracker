package com.habittracker.notifications.application;

import com.habittracker.groups.application.GroupProgress;
import com.habittracker.groups.application.GroupProgressService;
import com.habittracker.groups.application.GroupService;
import com.habittracker.groups.domain.Group;
import com.habittracker.notifications.domain.DeliveryStatus;
import com.habittracker.notifications.domain.NotificationDelivery;
import com.habittracker.notifications.domain.NotificationPreferences;
import com.habittracker.notifications.domain.NotificationRepository;
import com.habittracker.notifications.domain.NotificationSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyDigestServiceTest {

    /** Sunday 20 September 2026, 18:30 UTC. */
    private static final Instant SUNDAY_EVENING_UTC = Instant.parse("2026-09-20T18:30:00Z");
    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 14);

    @Mock
    private GroupService groupService;
    @Mock
    private GroupProgressService groupProgressService;
    @Mock
    private NotificationRepository repository;
    @Mock
    private NotificationSender sender;

    private final UUID maya = UUID.randomUUID();
    private final UUID dev = UUID.randomUUID();
    private final Group flat = Group.create("Flat 4B", maya, "Maya").addMember(maya, dev, "Dev");

    private WeeklyDigestService service() {
        return new WeeklyDigestService(groupService, groupProgressService,
            new NotificationPreferencesService(repository), repository, sender);
    }

    @Test
    void oneMembersFailedDeliveryIsRecordedAndDoesNotStopTheOthers() {
        when(groupService.allGroups()).thenReturn(List.of(flat));
        when(repository.findPreferences(any())).thenReturn(Optional.empty());
        when(groupProgressService.progress(eq(flat), any()))
            .thenReturn(new GroupProgress(flat, MONDAY, 0, 0, List.of(), List.of(), List.of()));
        doThrow(new IllegalStateException("mailbox full")).when(sender).send(eq(maya), any(), any());

        int sent = service().sendDue(SUNDAY_EVENING_UTC);

        assertThat(sent).isEqualTo(1);
        ArgumentCaptor<NotificationDelivery> recorded = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(repository, times(2)).recordDelivery(recorded.capture());
        assertThat(recorded.getAllValues())
            .extracting(NotificationDelivery::userId, NotificationDelivery::status, NotificationDelivery::error)
            .containsExactlyInAnyOrder(
                tuple(maya, DeliveryStatus.FAILED, "mailbox full"),
                tuple(dev, DeliveryStatus.SENT, null));
    }

    @Test
    void nothingIsDueBeforeSundayEveningInTheMembersTimezone() {
        when(groupService.allGroups()).thenReturn(List.of(flat));
        // 18:30 UTC on Sunday is already Monday 00:00 in Kolkata — the digest
        // window for that week has passed there; in Los Angeles it's 11:30.
        when(repository.findPreferences(maya)).thenReturn(
            Optional.of(new NotificationPreferences(maya, true, ZoneId.of("America/Los_Angeles"))));
        when(repository.findPreferences(dev)).thenReturn(
            Optional.of(new NotificationPreferences(dev, true, ZoneId.of("Asia/Kolkata"))));

        assertThat(service().sendDue(SUNDAY_EVENING_UTC)).isZero();
        verify(sender, never()).send(any(), any(), any());
    }

    @Test
    void membersWhoTurnedTheDigestOffAreSkipped() {
        when(groupService.allGroups()).thenReturn(List.of(flat));
        when(repository.findPreferences(any())).thenReturn(Optional.empty());
        when(repository.findPreferences(maya)).thenReturn(
            Optional.of(new NotificationPreferences(maya, false, ZoneId.of("UTC"))));
        when(groupProgressService.progress(eq(flat), any()))
            .thenReturn(new GroupProgress(flat, MONDAY, 0, 0, List.of(), List.of(), List.of()));

        assertThat(service().sendDue(SUNDAY_EVENING_UTC)).isEqualTo(1);
        verify(sender, never()).send(eq(maya), any(), any());
    }
}
