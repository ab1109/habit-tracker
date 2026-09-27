package com.habittracker.groups;

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

class GroupIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final UUID maya = UUID.randomUUID();
    private final UUID dev = UUID.randomUUID();
    private final UUID stranger = UUID.randomUUID();

    @Test
    void oneMembersCheckInCoversAJointHabitForTheWholeCircle() throws Exception {
        String groupId = createGroupWithDev("Flat 4B");
        String habitId = JsonPath.read(mockMvc.perform(post("/groups/" + groupId + "/habits")
                .header("X-User-Id", maya)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Kitchen reset\",\"scheduleType\":\"DAILY\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ownerType").value("GROUP"))
            .andReturn().getResponse().getContentAsString(), "$.id");

        String first = mockMvc.perform(post("/habits/" + habitId + "/checkins")
                .header("X-User-Id", maya).header("X-Timezone", "UTC"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.groupId").value(groupId))
            .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/habits/" + habitId + "/checkins")
                .header("X-User-Id", dev).header("X-Timezone", "UTC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value((String) JsonPath.read(first, "$.id")))
            .andExpect(jsonPath("$.performedByUserId").value(maya.toString()));

        mockMvc.perform(post("/habits/" + habitId + "/checkins")
                .header("X-User-Id", stranger).header("X-Timezone", "UTC"))
            .andExpect(status().isForbidden());

        // Dev can't erase Maya's coverage.
        mockMvc.perform(delete("/habits/" + habitId + "/checkins/today")
                .header("X-User-Id", dev).header("X-Timezone", "UTC"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/groups/" + groupId + "/progress")
                .header("X-User-Id", dev).header("X-Timezone", "UTC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jointHabits[0].progress.currentStreak").value(1))
            .andExpect(jsonPath("$.jointHabits[0].coverage[0].displayName").value("Maya"))
            .andExpect(jsonPath("$.jointHabits[0].coverage[0].count").value(1))
            .andExpect(jsonPath("$.recentActivity[0].displayName").value("Maya"))
            .andExpect(jsonPath("$.totalDone").value(1))
            .andExpect(jsonPath("$.totalTarget").value(7));
    }

    @Test
    void aSharedHabitIsVisibleToTheCircleButKeepsItsOwnersCheckIns() throws Exception {
        String groupId = createGroupWithDev("Morning Runners");
        String habitId = JsonPath.read(mockMvc.perform(post("/habits")
                .header("X-User-Id", maya)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Run\",\"scheduleType\":\"N_TIMES_PER_WEEK\",\"timesPerWeek\":4}"))
            .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/habits/" + habitId + "/progress")
                .header("X-User-Id", dev).header("X-Timezone", "UTC"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/groups/" + groupId + "/shared-habits")
                .header("X-User-Id", maya)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"habitId\":\"" + habitId + "\"}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/habits/" + habitId + "/progress")
                .header("X-User-Id", dev).header("X-Timezone", "UTC"))
            .andExpect(status().isOk());
        // Sharing is visibility only: Dev still can't check in on Maya's behalf.
        mockMvc.perform(post("/habits/" + habitId + "/checkins")
                .header("X-User-Id", dev).header("X-Timezone", "UTC"))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/habits/" + habitId + "/progress")
                .header("X-User-Id", stranger).header("X-Timezone", "UTC"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/groups/" + groupId + "/progress")
                .header("X-User-Id", dev).header("X-Timezone", "UTC"))
            .andExpect(jsonPath("$.sharedHabits[0].ownerDisplayName").value("Maya"))
            .andExpect(jsonPath("$.sharedHabits[0].progress.streakUnit").value("WEEKS"))
            .andExpect(jsonPath("$.totalTarget").value(4));
    }

    @Test
    void sharingSomeoneElsesHabitIsForbidden() throws Exception {
        String groupId = createGroupWithDev("Flat 4B");
        String devsHabit = JsonPath.read(mockMvc.perform(post("/habits")
                .header("X-User-Id", dev)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Read\",\"scheduleType\":\"DAILY\"}"))
            .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/groups/" + groupId + "/shared-habits")
                .header("X-User-Id", maya)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"habitId\":\"" + devsHabit + "\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void leavingACircleRevokesAccessAndUnsharesTheLeaversHabits() throws Exception {
        String groupId = createGroupWithDev("Flat 4B");
        String devsHabit = JsonPath.read(mockMvc.perform(post("/habits")
                .header("X-User-Id", dev)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Read\",\"scheduleType\":\"DAILY\"}"))
            .andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(post("/groups/" + groupId + "/shared-habits")
                .header("X-User-Id", dev)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"habitId\":\"" + devsHabit + "\"}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(delete("/groups/" + groupId + "/members/" + dev).header("X-User-Id", dev))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/groups/" + groupId).header("X-User-Id", dev))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/groups/" + groupId + "/progress")
                .header("X-User-Id", maya).header("X-Timezone", "UTC"))
            .andExpect(jsonPath("$.sharedHabits.length()").value(0));
        mockMvc.perform(get("/habits/" + devsHabit + "/progress")
                .header("X-User-Id", maya).header("X-Timezone", "UTC"))
            .andExpect(status().isForbidden());
    }

    @Test
    void listsOnlyTheCirclesTheCallerBelongsTo() throws Exception {
        createGroupWithDev("Flat 4B");

        mockMvc.perform(get("/groups").header("X-User-Id", dev))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].members.length()").value(2));
        mockMvc.perform(get("/groups").header("X-User-Id", stranger))
            .andExpect(jsonPath("$.length()").value(0));
    }

    private String createGroupWithDev(String name) throws Exception {
        String groupId = JsonPath.read(mockMvc.perform(post("/groups")
                .header("X-User-Id", maya)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\",\"displayName\":\"Maya\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/groups/" + groupId + "/members")
                .header("X-User-Id", maya)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"" + dev + "\",\"displayName\":\"Dev\"}"))
            .andExpect(status().isOk());
        return groupId;
    }
}
