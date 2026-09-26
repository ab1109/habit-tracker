package com.habittracker.habits.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habittracker.common.domain.OwnerType;
import com.habittracker.habits.domain.DailySchedule;
import com.habittracker.habits.domain.Habit;
import com.habittracker.habits.domain.NTimesPerWeekSchedule;
import com.habittracker.habits.domain.Schedule;
import com.habittracker.habits.domain.ScheduleType;
import com.habittracker.habits.domain.SpecificWeekdaysSchedule;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.Set;

@Component
class HabitMapper {

    private final ObjectMapper objectMapper;

    HabitMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    HabitJpaEntity toEntity(Habit habit) {
        return new HabitJpaEntity(
            habit.id(),
            habit.ownerType().name(),
            habit.ownerId(),
            habit.name(),
            habit.schedule().type().name(),
            scheduleToJson(habit.schedule()),
            habit.archivedAt(),
            habit.createdAt()
        );
    }

    Habit toDomain(HabitJpaEntity entity) {
        return new Habit(
            entity.getId(),
            OwnerType.valueOf(entity.getOwnerType()),
            entity.getOwnerId(),
            entity.getName(),
            scheduleFromJson(ScheduleType.valueOf(entity.getScheduleType()), entity.getScheduleParams()),
            entity.getArchivedAt(),
            entity.getCreatedAt()
        );
    }

    private String scheduleToJson(Schedule schedule) {
        try {
            return switch (schedule) {
                case DailySchedule ignored -> "{}";
                case NTimesPerWeekSchedule s ->
                    objectMapper.writeValueAsString(new NTimesPerWeekJson(s.timesPerWeek()));
                case SpecificWeekdaysSchedule s ->
                    objectMapper.writeValueAsString(new SpecificWeekdaysJson(s.weekdays()));
            };
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize schedule", e);
        }
    }

    private Schedule scheduleFromJson(ScheduleType type, String json) {
        try {
            return switch (type) {
                case DAILY -> new DailySchedule();
                case N_TIMES_PER_WEEK ->
                    new NTimesPerWeekSchedule(objectMapper.readValue(json, NTimesPerWeekJson.class).timesPerWeek());
                case SPECIFIC_WEEKDAYS ->
                    new SpecificWeekdaysSchedule(objectMapper.readValue(json, SpecificWeekdaysJson.class).weekdays());
            };
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize schedule", e);
        }
    }

    private record NTimesPerWeekJson(int timesPerWeek) {}

    private record SpecificWeekdaysJson(Set<DayOfWeek> weekdays) {}
}
