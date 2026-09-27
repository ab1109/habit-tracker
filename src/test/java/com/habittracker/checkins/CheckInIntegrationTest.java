package com.habittracker.checkins;

import com.habittracker.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckInIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void secondCheckInSameDayIsIdempotent() throws Exception {
        UUID userId = UUID.randomUUID();
        String habitId = createHabit(userId, "Meditate");

        String firstCheckIn = mockMvc.perform(post("/habits/" + habitId + "/checkins")
                .header("X-User-Id", userId)
                .header("X-Timezone", "America/Los_Angeles"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String firstId = JsonPath.read(firstCheckIn, "$.id");

        mockMvc.perform(post("/habits/" + habitId + "/checkins")
                .header("X-User-Id", userId)
                .header("X-Timezone", "America/Los_Angeles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(firstId));
    }

    @Test
    void aMistakenCheckInCanBeUndoneAndMadeAgain() throws Exception {
        UUID userId = UUID.randomUUID();
        String habitId = createHabit(userId, "Meditate");

        mockMvc.perform(post("/habits/" + habitId + "/checkins")
                .header("X-User-Id", userId).header("X-Timezone", "UTC"))
            .andExpect(status().isCreated());

        mockMvc.perform(delete("/habits/" + habitId + "/checkins/today")
                .header("X-User-Id", userId).header("X-Timezone", "UTC"))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/habits/" + habitId + "/progress")
                .header("X-User-Id", userId).header("X-Timezone", "UTC"))
            .andExpect(jsonPath("$.currentPeriodMet").value(false))
            .andExpect(jsonPath("$.currentStreak").value(0));

        // Undoing again changes nothing; checking in again creates a fresh record.
        mockMvc.perform(delete("/habits/" + habitId + "/checkins/today")
                .header("X-User-Id", userId).header("X-Timezone", "UTC"))
            .andExpect(status().isNoContent());
        mockMvc.perform(post("/habits/" + habitId + "/checkins")
                .header("X-User-Id", userId).header("X-Timezone", "UTC"))
            .andExpect(status().isCreated());
    }

    @Test
    void someoneElseCannotUndoMyCheckIn() throws Exception {
        UUID userId = UUID.randomUUID();
        String habitId = createHabit(userId, "Meditate");
        mockMvc.perform(post("/habits/" + habitId + "/checkins")
            .header("X-User-Id", userId).header("X-Timezone", "UTC"));

        mockMvc.perform(delete("/habits/" + habitId + "/checkins/today")
                .header("X-User-Id", UUID.randomUUID()).header("X-Timezone", "UTC"))
            .andExpect(status().isForbidden());
    }

    @Test
    void checkInForUnknownHabitReturns404() throws Exception {
        mockMvc.perform(post("/habits/" + UUID.randomUUID() + "/checkins")
                .header("X-User-Id", UUID.randomUUID())
                .header("X-Timezone", "UTC"))
            .andExpect(status().isNotFound());
    }

    @Test
    void invalidTimezoneReturns400() throws Exception {
        String habitId = createHabit(UUID.randomUUID(), "Read");

        mockMvc.perform(post("/habits/" + habitId + "/checkins")
                .header("X-User-Id", UUID.randomUUID())
                .header("X-Timezone", "Not/AZone"))
            .andExpect(status().isBadRequest());
    }

    private String createHabit(UUID userId, String name) throws Exception {
        String response = mockMvc.perform(post("/habits")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"scheduleType\":\"DAILY\"}"))
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
