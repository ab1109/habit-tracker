package com.habittracker.common.domain;

/**
 * A requested aggregate does not exist. The API layer maps this to a 404
 * without needing to know which module threw it.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
