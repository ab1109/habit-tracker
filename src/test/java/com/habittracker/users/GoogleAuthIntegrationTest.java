package com.habittracker.users;

import com.habittracker.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The deployed configuration: Google sign-in, sessions, CSRF. */
@ActiveProfiles("google")
@TestPropertySource(properties = {"GOOGLE_CLIENT_ID=test-client-id", "GOOGLE_CLIENT_SECRET=test-client-secret"})
class GoogleAuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static RequestPostProcessor signedInAs(String subject, String name) {
        return oidcLogin().idToken(token -> token
            .subject(subject)
            .claim("email", name.toLowerCase() + "@example.com")
            .claim("name", name));
    }

    @Test
    void theUiAndAuthConfigArePublicButTheApiNeedsASignIn() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/auth/config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mode").value("GOOGLE"))
            .andExpect(jsonPath("$.loginUrl").value("/oauth2/authorization/google"));

        mockMvc.perform(get("/habits"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Sign in required"));
        mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void theLoginUrlRedirectsToGoogle() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", startsWith("https://accounts.google.com/")));
    }

    @Test
    void aGoogleAccountMapsToOneStableUser() throws Exception {
        String subject = "google-" + UUID.randomUUID();
        String first = mockMvc.perform(get("/me").with(signedInAs(subject, "Maya")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Maya"))
            .andExpect(jsonPath("$.email").value("maya@example.com"))
            .andReturn().getResponse().getContentAsString();

        String mayaId = JsonPath.read(first, "$.userId");
        mockMvc.perform(get("/me").with(signedInAs(subject, "Maya")))
            .andExpect(jsonPath("$.userId").value(mayaId));
        mockMvc.perform(get("/me").with(signedInAs("google-" + UUID.randomUUID(), "Dev")))
            .andExpect(jsonPath("$.userId").value(not(mayaId)));
    }

    @Test
    void writesNeedACsrfTokenAndTheXUserIdHeaderIsIgnored() throws Exception {
        String subject = "google-" + UUID.randomUUID();
        String me = JsonPath.read(mockMvc.perform(get("/me").with(signedInAs(subject, "Maya")))
            .andReturn().getResponse().getContentAsString(), "$.userId");
        String body = "{\"name\":\"Meditate\",\"scheduleType\":\"DAILY\"}";

        mockMvc.perform(post("/habits").with(signedInAs(subject, "Maya"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());

        // Claiming to be someone else via the dev header has no effect.
        mockMvc.perform(post("/habits").with(signedInAs(subject, "Maya")).with(csrf())
                .header("X-User-Id", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ownerId").value(me));
    }

    @Test
    void logoutReturns204() throws Exception {
        mockMvc.perform(post("/logout").with(signedInAs("google-" + UUID.randomUUID(), "Maya")).with(csrf()))
            .andExpect(status().isNoContent());
    }
}
