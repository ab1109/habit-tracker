package com.habittracker.groups;

import com.habittracker.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InviteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UUID maya = UUID.randomUUID();
    private final UUID priya = UUID.randomUUID();

    @Test
    void aFriendJoinsThroughAnInviteLinkAndAcceptingTwiceIsHarmless() throws Exception {
        String groupId = createGroup();
        String token = JsonPath.read(mockMvc.perform(post("/groups/" + groupId + "/invites").header("X-User-Id", maya))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expiresAt").exists())
            .andReturn().getResponse().getContentAsString(), "$.token");

        mockMvc.perform(get("/invites/" + token).header("X-User-Id", priya))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.groupName").value("Morning Runners"))
            .andExpect(jsonPath("$.memberCount").value(1))
            .andExpect(jsonPath("$.alreadyMember").value(false));

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/invites/" + token + "/accept").header("X-User-Id", priya)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"Priya\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2));
        }

        mockMvc.perform(get("/groups/" + groupId).header("X-User-Id", priya)).andExpect(status().isOk());
        mockMvc.perform(get("/invites/" + token).header("X-User-Id", priya))
            .andExpect(jsonPath("$.alreadyMember").value(true));
    }

    @Test
    void onlyMembersCanCreateInviteLinks() throws Exception {
        String groupId = createGroup();

        mockMvc.perform(post("/groups/" + groupId + "/invites").header("X-User-Id", priya))
            .andExpect(status().isForbidden());
    }

    @Test
    void expiredAndUnknownLinksAreRejected() throws Exception {
        String groupId = createGroup();
        String token = JsonPath.read(mockMvc.perform(post("/groups/" + groupId + "/invites").header("X-User-Id", maya))
            .andReturn().getResponse().getContentAsString(), "$.token");
        jdbcTemplate.update("UPDATE group_invites SET expires_at = now() - interval '1 minute' WHERE token = ?", token);

        mockMvc.perform(post("/invites/" + token + "/accept").header("X-User-Id", priya)
                .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"Priya\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("This invite link is invalid or has expired"));
        mockMvc.perform(get("/invites/not-a-real-token").header("X-User-Id", priya))
            .andExpect(status().isNotFound());
    }

    private String createGroup() throws Exception {
        return JsonPath.read(mockMvc.perform(post("/groups").header("X-User-Id", maya)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Morning Runners\",\"displayName\":\"Maya\"}"))
            .andReturn().getResponse().getContentAsString(), "$.id");
    }
}
