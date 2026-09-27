package com.habittracker.checkins.application;

import com.habittracker.checkins.domain.CheckIn;

/**
 * @param alreadyExisted true if this call found an existing check-in rather
 *                        than creating a new one — the caller (the API layer)
 *                        uses this to choose between a 201 and a 200 response.
 */
public record CheckInResult(CheckIn checkIn, boolean alreadyExisted) {
}
