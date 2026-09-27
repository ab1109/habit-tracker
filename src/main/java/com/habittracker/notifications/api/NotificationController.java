package com.habittracker.notifications.api;

import com.habittracker.checkins.domain.LocalDateResolver;
import com.habittracker.notifications.application.NotificationPreferencesService;
import com.habittracker.notifications.application.WeeklyDigestService;
import com.habittracker.notifications.domain.DeliveryStatus;
import com.habittracker.notifications.domain.NotificationDelivery;
import com.habittracker.notifications.domain.NotificationKind;
import com.habittracker.notifications.domain.NotificationPreferences;
import com.habittracker.notifications.domain.WeeklyDigest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
public class NotificationController {

    private final NotificationPreferencesService preferencesService;
    private final WeeklyDigestService digestService;
    private final Clock clock;

    public NotificationController(NotificationPreferencesService preferencesService,
                                  WeeklyDigestService digestService, Clock clock) {
        this.preferencesService = preferencesService;
        this.digestService = digestService;
        this.clock = clock;
    }

    public record PreferencesBody(@NotNull Boolean weeklyDigestEnabled, @NotBlank String timezone) {

        static PreferencesBody from(NotificationPreferences p) {
            return new PreferencesBody(p.weeklyDigestEnabled(), p.timezone().getId());
        }
    }

    public record DeliveryResponse(UUID id, UUID groupId, NotificationKind kind, LocalDate periodStart,
                                   DeliveryStatus status, String subject, String body, Instant attemptedAt) {

        static DeliveryResponse from(NotificationDelivery d) {
            return new DeliveryResponse(d.id(), d.groupId(), d.kind(), d.periodStart(), d.status(),
                d.subject(), d.body(), d.attemptedAt());
        }
    }

    public record DigestResponse(UUID groupId, String groupName, LocalDate weekStart, LocalDate weekEnd,
                                 String subject, String body, List<WeeklyDigest.OnTarget> onTarget,
                                 List<WeeklyDigest.Behind> behind,
                                 List<WeeklyDigest.Uncovered> uncoveredJointHabits) {

        static DigestResponse from(WeeklyDigest d) {
            return new DigestResponse(d.groupId(), d.groupName(), d.weekStart(), d.weekEnd(), d.subject(),
                d.body(), d.onTarget(), d.behind(), d.uncoveredJointHabits());
        }
    }

    @GetMapping("/me/notification-preferences")
    public PreferencesBody preferences(@RequestHeader("X-User-Id") UUID userId) {
        return PreferencesBody.from(preferencesService.preferencesOf(userId));
    }

    @PutMapping("/me/notification-preferences")
    public PreferencesBody updatePreferences(
        @RequestHeader("X-User-Id") UUID userId,
        @Valid @RequestBody PreferencesBody body
    ) {
        NotificationPreferences updated = preferencesService.update(
            new NotificationPreferences(userId, body.weeklyDigestEnabled(), ZoneId.of(body.timezone())));
        return PreferencesBody.from(updated);
    }

    @GetMapping("/me/notifications")
    public List<DeliveryResponse> notifications(@RequestHeader("X-User-Id") UUID userId) {
        return preferencesService.deliveriesOf(userId).stream().map(DeliveryResponse::from).toList();
    }

    @GetMapping("/groups/{groupId}/digest")
    public DigestResponse digestPreview(
        @PathVariable UUID groupId,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestHeader("X-Timezone") String timezone
    ) {
        LocalDate today = LocalDateResolver.resolve(clock.instant(), ZoneId.of(timezone));
        return DigestResponse.from(digestService.preview(groupId, userId, today));
    }
}
