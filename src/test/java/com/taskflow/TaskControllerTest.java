package com.taskflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listReturnsSeededTasks() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[?(@.title == 'Draft the API contract')].status", hasItem("in-progress")))
                .andExpect(jsonPath("$[?(@.title == 'Draft the API contract')].priority", hasItem("high")))
                .andExpect(jsonPath("$[0].completed").doesNotExist());
    }

    @Test
    void getByIdReturnsOneTask() throws Exception {
        long id = firstTaskId();

        mockMvc.perform(get("/api/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").isNotEmpty());
    }

    @Test
    void createPersistsLowercaseStatusAndPriority() throws Exception {
        String body = """
                {
                  "title": "Review the pull request",
                  "description": "Check the task JSON before merging.",
                  "status": "in-progress",
                  "priority": "high"
                }
                """;

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Review the pull request"))
                .andExpect(jsonPath("$.status").value("in-progress"))
                .andExpect(jsonPath("$.priority").value("high"))
                .andExpect(jsonPath("$.completed").doesNotExist());
    }

    @Test
    void updateChangesStatus() throws Exception {
        long id = firstTaskId();
        String body = """
                {
                  "title": "Draft the API contract",
                  "description": "Fields are agreed.",
                  "status": "done",
                  "priority": "low"
                }
                """;

        mockMvc.perform(put("/api/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.priority").value("low"));
    }

    @Test
    void deleteRemovesTask() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Temporary task\",\"status\":\"todo\",\"priority\":\"low\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/tasks/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingTaskReturns404() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", 999999))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/tasks/{id}", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Missing\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/tasks/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void blankTitleIsRejected() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"status\":\"todo\",\"priority\":\"low\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void preflightFromAllowedOriginSucceeds() throws Exception {
        mockMvc.perform(options("/api/tasks")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("DELETE")));

        mockMvc.perform(get("/api/tasks")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private long firstTaskId() throws Exception {
        String body = mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode tasks = objectMapper.readTree(body);
        return tasks.get(0).get("id").asLong();
    }
}
