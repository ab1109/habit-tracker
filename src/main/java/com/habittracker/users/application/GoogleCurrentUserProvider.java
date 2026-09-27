package com.habittracker.users.application;

import com.habittracker.common.domain.UnauthorizedException;
import com.habittracker.users.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/** The signed-in Google account, mapped to our user (created on first sign-in). */
@Component
@Profile("google")
class GoogleCurrentUserProvider implements CurrentUserProvider {

    private final UserService userService;

    GoogleCurrentUserProvider(UserService userService) {
        this.userService = userService;
    }

    @Override
    public AuthMode mode() {
        return AuthMode.GOOGLE;
    }

    @Override
    public CurrentUserInfo currentUser(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof OidcUser oidc)) {
            throw new UnauthorizedException("Sign in required");
        }
        User user = userService.findOrCreateGoogleUser(oidc.getSubject(), oidc.getEmail(), oidc.getFullName());
        return new CurrentUserInfo(user.id(), user.displayName(), user.email());
    }
}
