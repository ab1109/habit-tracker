package com.habittracker.habits.infrastructure;

import com.habittracker.habits.domain.Habit;
import com.habittracker.habits.domain.HabitRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class HabitRepositoryAdapter implements HabitRepository {

    private final HabitJpaSpringDataRepository springDataRepository;
    private final HabitMapper mapper;

    HabitRepositoryAdapter(HabitJpaSpringDataRepository springDataRepository, HabitMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Habit save(Habit habit) {
        HabitJpaEntity saved = springDataRepository.save(mapper.toEntity(habit));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Habit> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }
}
