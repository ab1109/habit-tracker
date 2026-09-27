package com.habittracker.checkins.infrastructure;

import com.habittracker.checkins.domain.CheckIn;
import org.springframework.stereotype.Component;

@Component
class CheckInMapper {

    CheckIn toDomain(CheckInJpaEntity entity) {
        return new CheckIn(
            entity.getId(),
            entity.getHabitId(),
            entity.getGroupId(),
            entity.getUserId(),
            entity.getPerformedByUserId(),
            entity.getRecordedAt(),
            entity.getLocalDate()
        );
    }
}
