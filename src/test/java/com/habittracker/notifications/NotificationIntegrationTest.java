package com.habittracker.notifications;

import com.habittracker.notifications.application.WeeklyDigestService;
import com.habittracker.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationIntegrationTest extends AbstractIntegrationTest {

    /** A Sunday evening in UTC — when digests are due for members on the UTC default. */
    private static final Instant SUNDAY_EVENING_UTC = Instant.parse("2026-09-20T18:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WeeklyDigestService digestService;

    @Test
    void preferencesDefaultToDigestOnInUtcAndCanBeChanged() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/me/notification-preferences").header("X-User-Id", userId))
            .andExpect(jsonPath("$.weeklyDigestEnabled").value(true))
            .andExpect(jsonPath("$.timezone").value("UTC"));

        mockMvc.perform(put("/me/notification-preferences").header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weeklyDigestEnabled\":false,\"timezone\":\"Asia/Kolkata\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/me/notification-preferences").header("X-User-Id", userId))
            .andExpect(jsonPath("$.weeklyDigestEnabled").value(false))
            .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"));

        mockMvc.perform(put("/me/notification-preferences").header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weeklyDigestEnabled\":true,\"timezone\":\"Not/AZone\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void theWeeklyDigestIsSentOncePerMemberPerWeekEvenWhenTheJobReruns() throws Exception {
        UUID maya = UUID.randomUUID();
        String groupId = JsonPath.read(mockMvc.perform(post("/groups")
                .header("X-User-Id", maya)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Flat 4B\",\"displayName\":\"Maya\"}"))
            .andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(post("/groups/" + groupId + "/habits")
            .header("X-User-Id", maya)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Kitchen reset\",\"scheduleType\":\"DAILY\"}"));

        digestService.sendDue(SUNDAY_EVENING_UTC);
        digestService.sendDue(SUNDAY_EVENING_UTC.plusSeconds(3600));

        mockMvc.perform(get("/me/notifications").header("X-User-Id", maya))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("SENT"))
            .andExpect(jsonPath("$[0].periodStart").value("2026-09-14"))
            .andExpect(jsonPath("$[0].subject").value("Flat 4B — your week (14 Sep–20 Sep)"))
            .andExpect(jsonPath("$[0].body").value(containsString("Kitchen reset — 7 days")));
    }

    @Test
    void digestPreviewIsForMembersOnly() throws Exception {
        UUID maya = UUID.randomUUID();
        String groupId = JsonPath.read(mockMvc.perform(post("/groups")
                .header("X-User-Id", maya)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Morning Runners\",\"displayName\":\"Maya\"}"))
            .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/groups/" + groupId + "/digest")
                .header("X-User-Id", maya).header("X-Timezone", "UTC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.groupName").value("Morning Runners"))
            .andExpect(jsonPath("$.body").value("Nothing is shared with Morning Runners yet."));

        mockMvc.perform(get("/groups/" + groupId + "/digest")
                .header("X-User-Id", UUID.randomUUID()).header("X-Timezone", "UTC"))
            .andExpect(status().isForbidden());
    }
}
