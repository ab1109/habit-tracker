package com.habittracker.users.application;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Who is making this request. One implementation per auth mode; exactly one
 * is active, chosen by the {@code google} Spring profile.
 */
public interface CurrentUserProvider {

    AuthMode mode();

    /** @throws com.habittracker.common.domain.UnauthorizedException if nobody is signed in */
    CurrentUserInfo currentUser(HttpServletRequest request);

    enum AuthMode {
        /** Signed in with Google; identity comes from the session. */
        GOOGLE,
        /** Local development and tests only: the X-User-Id header is trusted as-is. */
        DEV_HEADER
    }

    /** @param displayName and {@code email} are null in dev-header mode */
    record CurrentUserInfo(java.util.UUID userId, String displayName, String email) {
    }
}
