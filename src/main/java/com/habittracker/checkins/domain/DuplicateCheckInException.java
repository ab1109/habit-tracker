package com.habittracker.checkins.domain;

/**
 * Signals that a check-in for this exact (habit, user, local date) already
 * exists. This is an internal signal between the persistence adapter and the
 * application service — the service catches it and returns the existing
 * check-in instead of an error; it should never reach the API layer as a
 * thrown exception.
 */
public class DuplicateCheckInException extends RuntimeException {

    public DuplicateCheckInException(String message, Throwable cause) {
        super(message, cause);
    }
}
