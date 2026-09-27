package com.habittracker.users.infrastructure;

import com.habittracker.users.domain.User;
import com.habittracker.users.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class UserRepositoryAdapter implements UserRepository {

    private final UserJpaSpringDataRepository springDataRepository;

    UserRepositoryAdapter(UserJpaSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<User> findByGoogleSubject(String googleSubject) {
        return springDataRepository.findByGoogleSubject(googleSubject)
            .map(e -> new User(e.getId(), e.getGoogleSubject(), e.getEmail(), e.getDisplayName(), e.getCreatedAt()));
    }

    @Override
    public void insertIfAbsent(User user) {
        springDataRepository.insertIfAbsent(user.id(), user.googleSubject(), user.email(), user.displayName(),
            user.createdAt());
    }
}
