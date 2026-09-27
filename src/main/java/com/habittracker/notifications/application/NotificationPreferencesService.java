package com.habittracker.notifications.application;

import com.habittracker.notifications.domain.NotificationDelivery;
import com.habittracker.notifications.domain.NotificationPreferences;
import com.habittracker.notifications.domain.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationPreferencesService {

    static final int DELIVERY_LOG_LIMIT = 50;

    private final NotificationRepository repository;

    public NotificationPreferencesService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public NotificationPreferences preferencesOf(UUID userId) {
        return repository.findPreferences(userId).orElseGet(() -> NotificationPreferences.defaults(userId));
    }

    @Transactional
    public NotificationPreferences update(NotificationPreferences preferences) {
        return repository.savePreferences(preferences);
    }

    @Transactional(readOnly = true)
    public List<NotificationDelivery> deliveriesOf(UUID userId) {
        return repository.findDeliveries(userId, DELIVERY_LOG_LIMIT);
    }
}
