package com.habittracker.users.application;

import com.habittracker.common.domain.DomainValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Local development and tests: trusts the X-User-Id header, so anyone can act
 * as anyone. Never active under the {@code google} profile, which is what
 * deployments run.
 */
@Component
@Profile("!google")
class HeaderCurrentUserProvider implements CurrentUserProvider {

    private static final Logger log = LoggerFactory.getLogger(HeaderCurrentUserProvider.class);

    HeaderCurrentUserProvider() {
        log.warn("Auth mode DEV_HEADER: the X-User-Id header is trusted as-is. "
            + "Run with the 'google' profile for anything other people can reach.");
    }

    @Override
    public AuthMode mode() {
        return AuthMode.DEV_HEADER;
    }

    @Override
    public CurrentUserInfo currentUser(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        if (header == null || header.isBlank()) {
            throw new DomainValidationException("X-User-Id header is required");
        }
        try {
            return new CurrentUserInfo(UUID.fromString(header.strip()), null, null);
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("X-User-Id must be a UUID");
        }
    }
}
