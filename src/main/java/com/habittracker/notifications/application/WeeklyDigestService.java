package com.habittracker.notifications.application;

import com.habittracker.checkins.domain.LocalDateResolver;
import com.habittracker.groups.application.GroupProgressService;
import com.habittracker.groups.application.GroupService;
import com.habittracker.groups.domain.Group;
import com.habittracker.groups.domain.Member;
import com.habittracker.notifications.domain.NotificationDelivery;
import com.habittracker.notifications.domain.NotificationKind;
import com.habittracker.notifications.domain.NotificationPreferences;
import com.habittracker.notifications.domain.NotificationRepository;
import com.habittracker.notifications.domain.NotificationSender;
import com.habittracker.notifications.domain.WeeklyDigest;
import com.habittracker.streaks.domain.ProgressCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The weekly digest workflow. It sits downstream of every other module: it
 * only reads their derived data and writes its own delivery log, so a
 * failure here can never affect habits, check-ins or circles.
 */
@Service
public class WeeklyDigestService {

    /** Local hour on Sunday from which a member's digest is due. */
    static final int SEND_HOUR = 18;

    private static final Logger log = LoggerFactory.getLogger(WeeklyDigestService.class);

    private final GroupService groupService;
    private final GroupProgressService groupProgressService;
    private final NotificationPreferencesService preferencesService;
    private final NotificationRepository repository;
    private final NotificationSender sender;

    public WeeklyDigestService(GroupService groupService, GroupProgressService groupProgressService,
                               NotificationPreferencesService preferencesService,
                               NotificationRepository repository, NotificationSender sender) {
        this.groupService = groupService;
        this.groupProgressService = groupProgressService;
        this.preferencesService = preferencesService;
        this.repository = repository;
        this.sender = sender;
    }

    /** What the digest for this circle looks like right now, from the viewer's point of view. */
    public WeeklyDigest preview(UUID groupId, UUID viewerId, LocalDate today) {
        return WeeklyDigestComposer.compose(groupProgressService.progress(groupId, viewerId, today));
    }

    /**
     * Sends every digest that is due at {@code now}: for each member of each
     * circle whose local time is Sunday at or after {@link #SEND_HOUR}, and
     * who hasn't already been sent this week's digest. Safe to call
     * repeatedly — that's how the hourly job catches up after downtime.
     *
     * <p>Delivery is at-least-once: if sending succeeds but recording it
     * fails, the next run sends again.
     *
     * @return the number of digests successfully sent
     */
    public int sendDue(Instant now) {
        int sent = 0;
        for (Group group : groupService.allGroups()) {
            // Progress depends on the member's local date; members in the same
            // timezone share one computation.
            Map<LocalDate, WeeklyDigest> digestsByDate = new HashMap<>();
            for (Member member : group.members()) {
                NotificationPreferences preferences = preferencesService.preferencesOf(member.userId());
                if (!preferences.weeklyDigestEnabled()) {
                    continue;
                }
                ZonedDateTime local = now.atZone(preferences.timezone());
                if (local.getDayOfWeek() != DayOfWeek.SUNDAY || local.getHour() < SEND_HOUR) {
                    continue;
                }
                LocalDate today = LocalDateResolver.resolve(now, preferences.timezone());
                LocalDate weekStart = ProgressCalculator.weekStartOf(today);
                if (repository.wasSent(member.userId(), group.id(), NotificationKind.WEEKLY_DIGEST, weekStart)) {
                    continue;
                }
                WeeklyDigest digest = digestsByDate.computeIfAbsent(today,
                    date -> WeeklyDigestComposer.compose(groupProgressService.progress(group, date)));
                if (deliver(member.userId(), digest, now)) {
                    sent++;
                }
            }
        }
        return sent;
    }

    private boolean deliver(UUID userId, WeeklyDigest digest, Instant now) {
        try {
            sender.send(userId, digest.subject(), digest.body());
            repository.recordDelivery(NotificationDelivery.sent(userId, digest.groupId(),
                NotificationKind.WEEKLY_DIGEST, digest.weekStart(), digest.subject(), digest.body(), now));
            return true;
        } catch (RuntimeException e) {
            log.warn("Weekly digest for user {} in circle {} failed", userId, digest.groupId(), e);
            try {
                repository.recordDelivery(NotificationDelivery.failed(userId, digest.groupId(),
                    NotificationKind.WEEKLY_DIGEST, digest.weekStart(), digest.subject(), digest.body(),
                    String.valueOf(e.getMessage()), now));
            } catch (RuntimeException recordFailure) {
                log.error("Could not record failed delivery for user {}", userId, recordFailure);
            }
            return false;
        }
    }
}
