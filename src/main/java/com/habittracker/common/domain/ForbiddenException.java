package com.habittracker.common.domain;

/**
 * The caller is identified but not allowed to perform this action on the
 * resource (e.g. checking in to someone else's personal habit). The API
 * layer maps this to a 403 without needing to know which module threw it.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
