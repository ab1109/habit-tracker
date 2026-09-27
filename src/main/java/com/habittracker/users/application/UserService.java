package com.habittracker.users.application;

import com.habittracker.users.domain.User;
import com.habittracker.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** The account for this Google identity, created on first sign-in. */
    @Transactional
    public User findOrCreateGoogleUser(String googleSubject, String email, String displayName) {
        return userRepository.findByGoogleSubject(googleSubject).orElseGet(() -> {
            userRepository.insertIfAbsent(User.fromGoogle(googleSubject, email, displayName));
            return userRepository.findByGoogleSubject(googleSubject)
                .orElseThrow(() -> new IllegalStateException("User vanished right after insert"));
        });
    }
}
