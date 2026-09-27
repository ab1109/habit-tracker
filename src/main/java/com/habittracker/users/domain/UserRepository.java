package com.habittracker.users.domain;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findByGoogleSubject(String googleSubject);

    /**
     * Inserts unless an account for the same Google subject exists (two first
     * requests can race); either way, a later {@link #findByGoogleSubject} finds one.
     */
    void insertIfAbsent(User user);
}
