package com.habittracker.habits.domain;

import com.habittracker.common.domain.DomainValidationException;

public class InvalidScheduleException extends DomainValidationException {

    public InvalidScheduleException(String message) {
        super(message);
    }
}
