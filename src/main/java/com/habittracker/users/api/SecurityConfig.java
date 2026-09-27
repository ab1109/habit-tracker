package com.habittracker.users.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habittracker.common.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Two mutually exclusive setups:
 * <ul>
 *   <li>{@code google} profile (deployments): Google sign-in with a session
 *       cookie; every API call needs a signed-in session (401 otherwise), and
 *       every state-changing call needs the CSRF token the browser reads from
 *       the {@code XSRF-TOKEN} cookie and echoes in {@code X-XSRF-TOKEN}.</li>
 *   <li>otherwise (local development, tests): everything is open, and identity
 *       comes from the X-User-Id header — see {@code HeaderCurrentUserProvider}.</li>
 * </ul>
 */
@Configuration
class SecurityConfig {

    @Bean
    @Profile("google")
    SecurityFilterChain googleSecurity(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, e) ->
            writeJson(response, objectMapper, HttpStatus.UNAUTHORIZED, "Sign in required");

        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/favicon.ico", "/auth/config", "/error")
                .permitAll()
                .anyRequest().authenticated())
            // The UI navigates to the Google login URL itself; API calls just get a 401.
            .exceptionHandling(e -> e.authenticationEntryPoint(unauthorized))
            .oauth2Login(login -> login
                .defaultSuccessUrl("/", true)
                .failureUrl("/?signin=failed"))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
            .build();
    }

    @Bean
    @Profile("!google")
    SecurityFilterChain devHeaderSecurity(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .build();
    }

    private static void writeJson(HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status,
                                  String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiError(message));
    }

    /**
     * CSRF for a single-page app (the recipe from the Spring Security docs):
     * the token is read raw from the X-XSRF-TOKEN header the JavaScript sends,
     * while server-rendered forms (Spring's logout/login pages) keep the
     * BREACH-safe XOR encoding. Touching the token on every request makes
     * Spring write the XSRF-TOKEN cookie so the UI always has one.
     */
    static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String header = request.getHeader(csrfToken.getHeaderName());
            return (StringUtils.hasText(header) ? plain : xor).resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
