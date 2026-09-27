package com.habittracker.users.api;

import com.habittracker.users.application.CurrentUserProvider;
import com.habittracker.users.application.CurrentUserProvider.AuthMode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class AuthController {

    static final String GOOGLE_LOGIN_URL = "/oauth2/authorization/google";

    private final CurrentUserProvider provider;

    public AuthController(CurrentUserProvider provider) {
        this.provider = provider;
    }

    public record AuthConfigResponse(AuthMode mode, String loginUrl) {
    }

    public record MeResponse(UUID userId, String displayName, String email) {
    }

    /** Public: tells the UI which sign-in flow to show. */
    @GetMapping("/auth/config")
    public AuthConfigResponse config() {
        return new AuthConfigResponse(provider.mode(), provider.mode() == AuthMode.GOOGLE ? GOOGLE_LOGIN_URL : null);
    }

    @GetMapping("/me")
    public MeResponse me(HttpServletRequest request) {
        var user = provider.currentUser(request);
        return new MeResponse(user.userId(), user.displayName(), user.email());
    }
}
