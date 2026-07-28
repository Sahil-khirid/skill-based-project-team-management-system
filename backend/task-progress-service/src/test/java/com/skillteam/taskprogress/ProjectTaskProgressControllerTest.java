package com.skillteam.taskprogress;

import com.skillteam.taskprogress.entity.Task;
import com.skillteam.taskprogress.entity.TaskPriority;
import com.skillteam.taskprogress.entity.TaskStatus;
import com.skillteam.taskprogress.repository.TaskRepository;
import com.skillteam.taskprogress.security.IdentityHeaderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectTaskProgressControllerTest {

    private static final String ID_HEADER = IdentityHeaderResolver.USER_ID_HEADER;
    private static final String ROLE_HEADER = IdentityHeaderResolver.USER_ROLE_HEADER;
    private static final String MANAGER = "PROJECT_MANAGER";
    private static final String USER = "USER";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @AfterEach
    void cleanUp() {
        taskRepository.deleteAll();
    }

    private Task saveTask(Long projectId, TaskStatus status) {
        return taskRepository.saveAndFlush(new Task(projectId, "Task", null, status, TaskPriority.LOW, null));
    }

    private String summaryUrl(Long projectId) {
        return "/api/v1/projects/" + projectId + "/task-progress-summary";
    }

    private MockHttpServletRequestBuilder asManager(MockHttpServletRequestBuilder builder) {
        return builder.header(ID_HEADER, "1").header(ROLE_HEADER, MANAGER);
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder) {
        return builder.header(ID_HEADER, "2").header(ROLE_HEADER, USER);
    }

    // --- zero tasks ---

    @Test
    void projectWithNoTasksReturnsAllZeroSummary() throws Exception {
        mockMvc.perform(asManager(get(summaryUrl(42L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(42))
                .andExpect(jsonPath("$.totalTasks").value(0))
                .andExpect(jsonPath("$.todoCount").value(0))
                .andExpect(jsonPath("$.inProgressCount").value(0))
                .andExpect(jsonPath("$.completedCount").value(0))
                .andExpect(jsonPath("$.blockedCount").value(0))
                .andExpect(jsonPath("$.completionPercentage").value(0.0));
    }

    // --- mixed statuses ---

    @Test
    void projectWithMixedStatusTasksReturnsAggregateCounts() throws Exception {
        saveTask(7L, TaskStatus.TODO);
        saveTask(7L, TaskStatus.TODO);
        saveTask(7L, TaskStatus.IN_PROGRESS);
        saveTask(7L, TaskStatus.COMPLETED);
        saveTask(7L, TaskStatus.BLOCKED);

        mockMvc.perform(asManager(get(summaryUrl(7L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(7))
                .andExpect(jsonPath("$.totalTasks").value(5))
                .andExpect(jsonPath("$.todoCount").value(2))
                .andExpect(jsonPath("$.inProgressCount").value(1))
                .andExpect(jsonPath("$.completedCount").value(1))
                .andExpect(jsonPath("$.blockedCount").value(1))
                .andExpect(jsonPath("$.completionPercentage").value(20.0));
    }

    // --- all completed ---

    @Test
    void projectWithAllTasksCompletedReturnsFullCompletionPercentage() throws Exception {
        saveTask(8L, TaskStatus.COMPLETED);
        saveTask(8L, TaskStatus.COMPLETED);

        mockMvc.perform(asManager(get(summaryUrl(8L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionPercentage").value(100.0));
    }

    // --- tasks from other projects are excluded ---

    @Test
    void tasksFromOtherProjectsAreExcludedFromSummary() throws Exception {
        saveTask(9L, TaskStatus.COMPLETED);
        saveTask(10L, TaskStatus.TODO);
        saveTask(10L, TaskStatus.TODO);

        mockMvc.perform(asManager(get(summaryUrl(9L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(1))
                .andExpect(jsonPath("$.completedCount").value(1))
                .andExpect(jsonPath("$.todoCount").value(0));
    }

    // --- authorization: any authenticated role can read ---

    @Test
    void userRoleCanViewProgressSummary() throws Exception {
        mockMvc.perform(asUser(get(summaryUrl(1L))))
                .andExpect(status().isOk());
    }

    // --- missing/unsupported identity headers ---

    @Test
    void missingUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(summaryUrl(1L)).header(ROLE_HEADER, MANAGER))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void missingRoleHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(summaryUrl(1L)).header(ID_HEADER, "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unsupportedRoleHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(summaryUrl(1L)).header(ID_HEADER, "1").header(ROLE_HEADER, "ADMIN"))
                .andExpect(status().isUnauthorized());
    }
}
