package com.habittracker.notifications.infrastructure;

import com.habittracker.notifications.domain.DeliveryStatus;
import com.habittracker.notifications.domain.NotificationDelivery;
import com.habittracker.notifications.domain.NotificationKind;
import com.habittracker.notifications.domain.NotificationPreferences;
import com.habittracker.notifications.domain.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationPreferencesSpringDataRepository preferences;
    private final NotificationDeliverySpringDataRepository deliveries;

    NotificationRepositoryAdapter(NotificationPreferencesSpringDataRepository preferences,
                                  NotificationDeliverySpringDataRepository deliveries) {
        this.preferences = preferences;
        this.deliveries = deliveries;
    }

    @Override
    public Optional<NotificationPreferences> findPreferences(UUID userId) {
        return preferences.findById(userId).map(e ->
            new NotificationPreferences(e.getUserId(), e.isWeeklyDigestEnabled(), ZoneId.of(e.getTimezone())));
    }

    @Override
    public NotificationPreferences savePreferences(NotificationPreferences p) {
        preferences.save(new NotificationPreferencesJpaEntity(
            p.userId(), p.weeklyDigestEnabled(), p.timezone().getId(), Instant.now()));
        return p;
    }

    @Override
    public void recordDelivery(NotificationDelivery d) {
        deliveries.save(new NotificationDeliveryJpaEntity(d.id(), d.userId(), d.groupId(), d.kind().name(),
            d.periodStart(), d.status().name(), d.subject(), d.body(), d.error(), d.attemptedAt()));
    }

    @Override
    public boolean wasSent(UUID userId, UUID groupId, NotificationKind kind, LocalDate periodStart) {
        return deliveries.existsByUserIdAndGroupIdAndKindAndPeriodStartAndStatus(
            userId, groupId, kind.name(), periodStart, DeliveryStatus.SENT.name());
    }

    @Override
    public List<NotificationDelivery> findDeliveries(UUID userId, int limit) {
        return deliveries.findByUserIdOrderByAttemptedAtDesc(userId, PageRequest.of(0, limit)).stream()
            .map(e -> new NotificationDelivery(e.getId(), e.getUserId(), e.getGroupId(),
                NotificationKind.valueOf(e.getKind()), e.getPeriodStart(), DeliveryStatus.valueOf(e.getStatus()),
                e.getSubject(), e.getBody(), e.getError(), e.getAttemptedAt()))
            .toList();
    }
}
