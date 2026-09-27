package com.habittracker.streaks;

import com.habittracker.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HabitProgressIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void checkingInTodayStartsADailyStreakAndShowsInHistory() throws Exception {
        UUID userId = UUID.randomUUID();
        String habitId = createHabit(userId);
        String today = LocalDate.now(ZoneId.of("UTC")).toString();

        mockMvc.perform(post("/habits/" + habitId + "/checkins")
                .header("X-User-Id", userId).header("X-Timezone", "UTC"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/habits/" + habitId + "/progress")
                .header("X-User-Id", userId).header("X-Timezone", "UTC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.streakUnit").value("DAYS"))
            .andExpect(jsonPath("$.currentStreak").value(1))
            .andExpect(jsonPath("$.currentPeriodMet").value(true))
            .andExpect(jsonPath("$.thisWeek.target").value(7))
            .andExpect(jsonPath("$.weeks.length()").value(12));

        mockMvc.perform(get("/habits/" + habitId + "/checkins")
                .header("X-User-Id", userId)
                .param("from", today).param("to", today))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].localDate").value(today));
    }

    @Test
    void anotherUserCannotReadOrCheckInToAPersonalHabit() throws Exception {
        String habitId = createHabit(UUID.randomUUID());
        UUID stranger = UUID.randomUUID();

        mockMvc.perform(get("/habits/" + habitId + "/progress")
                .header("X-User-Id", stranger).header("X-Timezone", "UTC"))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/habits/" + habitId + "/checkins")
                .header("X-User-Id", stranger).header("X-Timezone", "UTC"))
            .andExpect(status().isForbidden());
    }

    @Test
    void listsOnlyTheCallersActiveHabits() throws Exception {
        UUID userId = UUID.randomUUID();
        createHabit(userId);
        String archived = createHabit(userId);
        createHabit(UUID.randomUUID());
        mockMvc.perform(patch("/habits/" + archived + "/archive").header("X-User-Id", userId));

        mockMvc.perform(get("/habits").header("X-User-Id", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void invertedDateRangeIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        String habitId = createHabit(userId);

        mockMvc.perform(get("/habits/" + habitId + "/checkins")
                .header("X-User-Id", userId)
                .param("from", "2026-09-10").param("to", "2026-09-01"))
            .andExpect(status().isBadRequest());
    }

    private String createHabit(UUID userId) throws Exception {
        String response = mockMvc.perform(post("/habits")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Meditate\",\"scheduleType\":\"DAILY\"}"))
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
