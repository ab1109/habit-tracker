package com.habittracker.habits;

import com.habittracker.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HabitIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createThenFetchRoundTripsThroughRealPostgres() throws Exception {
        String createResponse = mockMvc.perform(post("/habits")
                .header("X-User-Id", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Meditate","scheduleType":"DAILY"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Meditate"))
            .andExpect(jsonPath("$.scheduleType").value("DAILY"))
            .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(get("/habits/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Meditate"));
    }

    @Test
    void archivingSetsArchivedAt() throws Exception {
        String createResponse = mockMvc.perform(post("/habits")
                .header("X-User-Id", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Gym","scheduleType":"N_TIMES_PER_WEEK","timesPerWeek":3}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(patch("/habits/" + id + "/archive"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.archivedAt").exists());
    }

    @Test
    void invalidScheduleReturns400WithDomainMessage() throws Exception {
        mockMvc.perform(post("/habits")
                .header("X-User-Id", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Bad","scheduleType":"N_TIMES_PER_WEEK","timesPerWeek":10}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("timesPerWeek must be between 1 and 7, got 10"));
    }
}
