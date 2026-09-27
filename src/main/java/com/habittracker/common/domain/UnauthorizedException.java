package com.habittracker.common.domain;

/** The caller isn't signed in. The API layer maps this to a 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
