package com.habittracker.checkins;

import com.habittracker.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Fires many simultaneous check-ins for the same day through the full stack
 * and proves the database ends up with exactly one row, and every caller
 * gets a success response (201 for the winner, 200 for the rest).
 */
class ConcurrentCheckInIntegrationTest extends AbstractIntegrationTest {

    private static final int REQUESTS = 8;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void simultaneousPersonalCheckInsCreateExactlyOneRow() throws Exception {
        UUID userId = UUID.randomUUID();
        String habitId = JsonPath.read(mockMvc.perform(post("/habits")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Meditate\",\"scheduleType\":\"DAILY\"}"))
            .andReturn().getResponse().getContentAsString(), "$.id");

        List<Integer> statuses = fireConcurrently(i -> userId, habitId);

        assertThat(statuses).containsOnly(200, 201).containsOnlyOnce(201);
        assertThat(rowsFor(habitId)).isEqualTo(1);
    }

    @Test
    void simultaneousJointCheckInsByDifferentMembersCreateExactlyOneRow() throws Exception {
        List<UUID> members = new ArrayList<>();
        for (int i = 0; i < REQUESTS; i++) {
            members.add(UUID.randomUUID());
        }
        String groupId = JsonPath.read(mockMvc.perform(post("/groups")
                .header("X-User-Id", members.getFirst())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Flat 4B\",\"displayName\":\"M0\"}"))
            .andReturn().getResponse().getContentAsString(), "$.id");
        for (int i = 1; i < REQUESTS; i++) {
            mockMvc.perform(post("/groups/" + groupId + "/members")
                .header("X-User-Id", members.getFirst())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"" + members.get(i) + "\",\"displayName\":\"M" + i + "\"}"));
        }
        String habitId = JsonPath.read(mockMvc.perform(post("/groups/" + groupId + "/habits")
                .header("X-User-Id", members.getFirst())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Kitchen reset\",\"scheduleType\":\"DAILY\"}"))
            .andReturn().getResponse().getContentAsString(), "$.id");

        List<Integer> statuses = fireConcurrently(members::get, habitId);

        assertThat(statuses).containsOnly(200, 201).containsOnlyOnce(201);
        assertThat(rowsFor(habitId)).isEqualTo(1);
    }

    private List<Integer> fireConcurrently(java.util.function.IntFunction<UUID> userFor, String habitId)
        throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                UUID userId = userFor.apply(i);
                Callable<Integer> call = () -> {
                    start.await();
                    return mockMvc.perform(post("/habits/" + habitId + "/checkins")
                            .header("X-User-Id", userId).header("X-Timezone", "UTC"))
                        .andReturn().getResponse().getStatus();
                };
                futures.add(pool.submit(call));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    private int rowsFor(String habitId) {
        return jdbcTemplate.queryForObject(
            "SELECT count(*) FROM checkins WHERE habit_id = ?::uuid", Integer.class, habitId);
    }
}
