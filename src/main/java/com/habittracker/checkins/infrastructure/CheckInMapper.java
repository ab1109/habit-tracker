package com.habittracker.checkins.infrastructure;

import com.habittracker.checkins.domain.CheckIn;
import org.springframework.stereotype.Component;

@Component
class CheckInMapper {

    CheckInJpaEntity toEntity(CheckIn checkIn) {
        return new CheckInJpaEntity(
            checkIn.id(),
            checkIn.habitId(),
            null, // group_id: not used until slice 4
            checkIn.userId(),
            checkIn.performedByUserId(),
            checkIn.recordedAt(),
            checkIn.localDate()
        );
    }

    CheckIn toDomain(CheckInJpaEntity entity) {
        return new CheckIn(
            entity.getId(),
            entity.getHabitId(),
            entity.getUserId(),
            entity.getPerformedByUserId(),
            entity.getRecordedAt(),
            entity.getLocalDate()
        );
    }
}
