package com.skillteam.taskprogress;

import com.skillteam.taskprogress.client.ProjectTeamServiceClient;
import com.skillteam.taskprogress.client.RemoteProjectMemberResponse;
import com.skillteam.taskprogress.exception.ProjectTeamServiceUnavailableException;
import com.skillteam.taskprogress.repository.TaskRepository;
import com.skillteam.taskprogress.security.IdentityHeaderResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTest {

    private static final String TASKS_URL = "/api/v1/tasks";
    private static final String ID_HEADER = IdentityHeaderResolver.USER_ID_HEADER;
    private static final String ROLE_HEADER = IdentityHeaderResolver.USER_ROLE_HEADER;
    private static final String MANAGER = "PROJECT_MANAGER";
    private static final String USER = "USER";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @MockitoBean
    private ProjectTeamServiceClient projectTeamServiceClient;

    @AfterEach
    void cleanUp() {
        taskRepository.deleteAll();
    }

    private RemoteProjectMemberResponse remoteMember(Long projectId, Long authUserId) {
        return new RemoteProjectMemberResponse(authUserId, projectId, authUserId, "MEMBER", Instant.now(), Instant.now());
    }

    private String createBody(Long projectId, String title, String description, String priority, String dueDate) {
        return "{\"projectId\":" + projectId + ",\"title\":" + quoteOrNull(title)
                + ",\"description\":" + quoteOrNull(description) + ",\"priority\":" + quoteOrNull(priority)
                + ",\"dueDate\":" + quoteOrNull(dueDate) + "}";
    }

    private String updateBody(String title, String description, String status, String priority, String dueDate) {
        return "{\"title\":" + quoteOrNull(title) + ",\"description\":" + quoteOrNull(description)
                + ",\"status\":" + quoteOrNull(status) + ",\"priority\":" + quoteOrNull(priority)
                + ",\"dueDate\":" + quoteOrNull(dueDate) + "}";
    }

    private String quoteOrNull(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private String assignBody(Long assignedAuthUserId) {
        return "{\"assignedAuthUserId\":" + assignedAuthUserId + "}";
    }

    private String statusBody(String status) {
        return "{\"status\":" + quoteOrNull(status) + "}";
    }

    private String progressBody(Integer progressPercentage) {
        return "{\"progressPercentage\":" + progressPercentage + "}";
    }

    private MockHttpServletRequestBuilder asManager(MockHttpServletRequestBuilder builder) {
        return builder.header(ID_HEADER, "1").header(ROLE_HEADER, MANAGER);
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder) {
        return builder.header(ID_HEADER, "2").header(ROLE_HEADER, USER);
    }

    private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder, long authUserId) {
        return builder.header(ID_HEADER, String.valueOf(authUserId)).header(ROLE_HEADER, USER);
    }

    // --- create success ---

    @Test
    void createWithValidRequestReturnsCreated() throws Exception {
        mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Design schema", "Design the tasks table", "HIGH", "2026-08-01")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.title").value("Design schema"))
                .andExpect(jsonPath("$.description").value("Design the tasks table"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.dueDate").value("2026-08-01"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void createWithoutDueDateReturnsCreated() throws Exception {
        mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Draft API", null, "MEDIUM", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dueDate").doesNotExist());
    }

    // --- list ---

    @Test
    void listReturnsCreatedTasksInCreationOrder() throws Exception {
        mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "First task", null, "LOW", null))).andExpect(status().isCreated());
        mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Second task", null, "LOW", null))).andExpect(status().isCreated());

        mockMvc.perform(asManager(get(TASKS_URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("First task"))
                .andExpect(jsonPath("$[1].title").value("Second task"));
    }

    // --- get success ---

    @Test
    void getExistingTaskReturnsOk() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Implement service", "Task service layer", "MEDIUM", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(get(TASKS_URL + "/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Implement service"))
                .andExpect(jsonPath("$.description").value("Task service layer"));
    }

    // --- get absent ---

    @Test
    void getMissingTaskReturnsNotFound() throws Exception {
        mockMvc.perform(asManager(get(TASKS_URL + "/999999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // --- update success ---

    @Test
    void updateExistingTaskReturnsOk() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Old title", "Old description", "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(put(TASKS_URL + "/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("New title", "New description", "IN_PROGRESS", "HIGH", "2026-09-15")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.description").value("New description"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.dueDate").value("2026-09-15"));
    }

    // --- update absent ---

    @Test
    void updateMissingTaskReturnsNotFound() throws Exception {
        mockMvc.perform(asManager(put(TASKS_URL + "/999999")).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("Anything", null, "TODO", "LOW", null)))
                .andExpect(status().isNotFound());
    }

    // --- delete success ---

    @Test
    void deleteExistingTaskReturnsNoContent() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Removable task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(delete(TASKS_URL + "/" + id)))
                .andExpect(status().isNoContent());
    }

    // --- deleted task hidden from list and GET ---

    @Test
    void deletedTaskIsHiddenFromListAndGet() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Temporary task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(delete(TASKS_URL + "/" + id))).andExpect(status().isNoContent());

        mockMvc.perform(asManager(get(TASKS_URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(asManager(get(TASKS_URL + "/" + id)))
                .andExpect(status().isNotFound());
    }

    // --- repeated delete returns 404 ---

    @Test
    void repeatedDeleteReturnsNotFound() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Skylab task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(delete(TASKS_URL + "/" + id))).andExpect(status().isNoContent());

        mockMvc.perform(asManager(delete(TASKS_URL + "/" + id)))
                .andExpect(status().isNotFound());
    }

    // --- validation failures ---

    @Test
    void createWithBlankTitleReturnsBadRequest() throws Exception {
        mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "   ", null, "LOW", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createWithTooLongTitleReturnsBadRequest() throws Exception {
        String tooLong = "A".repeat(151);
        mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, tooLong, null, "LOW", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithoutProjectIdReturnsBadRequest() throws Exception {
        mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"No project\",\"priority\":\"LOW\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithoutPriorityReturnsBadRequest() throws Exception {
        mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":1,\"title\":\"No priority\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithInvalidPriorityReturnsBadRequest() throws Exception {
        mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":1,\"title\":\"Bad priority\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWithMissingStatusReturnsBadRequest() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Hubble task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(put(TASKS_URL + "/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hubble task\",\"priority\":\"LOW\",\"status\":null}"))
                .andExpect(status().isBadRequest());
    }

    // --- missing/malformed/zero/negative user ID ---

    @Test
    void createWithoutUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(TASKS_URL).header(ROLE_HEADER, MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "No id", null, "LOW", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void createWithMalformedUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(TASKS_URL).header(ID_HEADER, "not-a-number").header(ROLE_HEADER, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(1L, "Malformed id", null, "LOW", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithZeroUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(TASKS_URL).header(ID_HEADER, "0").header(ROLE_HEADER, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(1L, "Zero id", null, "LOW", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithNegativeUserIdHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(TASKS_URL).header(ID_HEADER, "-3").header(ROLE_HEADER, MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(1L, "Negative id", null, "LOW", null)))
                .andExpect(status().isUnauthorized());
    }

    // --- missing/unsupported role ---

    @Test
    void createWithoutRoleHeaderReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(TASKS_URL).header(ID_HEADER, "1").contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "No role", null, "LOW", null)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required."));
    }

    @Test
    void createWithUnsupportedRoleReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(TASKS_URL).header(ID_HEADER, "1").header(ROLE_HEADER, "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content(createBody(1L, "Bad role", null, "LOW", null)))
                .andExpect(status().isUnauthorized());
    }

    // --- USER can read ---

    @Test
    void userRoleCanListTasks() throws Exception {
        mockMvc.perform(asUser(get(TASKS_URL)))
                .andExpect(status().isOk());
    }

    @Test
    void userRoleCanGetTaskById() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Readable task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asUser(get(TASKS_URL + "/" + id)))
                .andExpect(status().isOk());
    }

    // --- USER receives 403 for POST, PUT and DELETE ---

    @Test
    void userRoleCannotCreateTask() throws Exception {
        mockMvc.perform(asUser(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Forbidden", null, "LOW", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access is denied."));
    }

    @Test
    void userRoleCannotUpdateTask() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Original", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asUser(put(TASKS_URL + "/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("Edited", null, "IN_PROGRESS", "LOW", null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userRoleCannotDeleteTask() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Protected", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asUser(delete(TASKS_URL + "/" + id)))
                .andExpect(status().isForbidden());
    }

    // --- PROJECT_MANAGER can perform write operations ---

    @Test
    void projectManagerCanCreateUpdateAndDeleteTask() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Full flow", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(put(TASKS_URL + "/" + id)).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("Full flow updated", null, "COMPLETED", "HIGH", null)))
                .andExpect(status().isOk());

        mockMvc.perform(asManager(delete(TASKS_URL + "/" + id)))
                .andExpect(status().isNoContent());
    }

    // --- assignment ---

    @Test
    void assignExistingTaskToAProjectMemberReturnsUpdatedTask() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Assignable task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        when(projectTeamServiceClient.fetchMembers(eq(1L), any(), any()))
                .thenReturn(List.of(remoteMember(1L, 5L)));

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/assign")).contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedAuthUserId").value(5));
    }

    @Test
    void assignMissingTaskReturnsNotFound() throws Exception {
        mockMvc.perform(asManager(patch(TASKS_URL + "/999999/assign")).contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(5L)))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignWithoutAssignedAuthUserIdReturnsBadRequest() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Assignable task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/assign")).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignToUserWhoIsNotAProjectMemberReturnsBadRequest() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Assignable task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        when(projectTeamServiceClient.fetchMembers(eq(1L), any(), any()))
                .thenReturn(List.of(remoteMember(1L, 20L)));

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/assign")).contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(5L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void assignWhenProjectTeamServiceIsUnavailableReturnsServiceUnavailable() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Assignable task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        when(projectTeamServiceClient.fetchMembers(eq(1L), any(), any()))
                .thenThrow(new ProjectTeamServiceUnavailableException("downstream unavailable", new RuntimeException()));

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/assign")).contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(5L)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void userRoleCannotAssignTask() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Protected task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asUser(patch(TASKS_URL + "/" + id + "/assign")).contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(5L)))
                .andExpect(status().isForbidden());
    }

    // --- status transitions ---

    @Test
    void updateStatusWithValidTransitionReturnsOk() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Status task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PROGRESS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void updateStatusToCompletedSetsProgressToOneHundred() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Completable task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PROGRESS")))
                .andExpect(status().isOk());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("COMPLETED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressPercentage").value(100));
    }

    @Test
    void updateStatusFromCompletedToTodoReturnsBadRequest() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Locked task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("COMPLETED")))
                .andExpect(status().isOk());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("TODO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateStatusMissingTaskReturnsNotFound() throws Exception {
        mockMvc.perform(asManager(patch(TASKS_URL + "/999999/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PROGRESS")))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatusWithInvalidStatusValueReturnsBadRequest() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Bad status task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userRoleCannotUpdateTaskStatus() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Protected status task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asUser(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PROGRESS")))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignedUserCanUpdateOwnTaskStatus() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Assigned status task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        when(projectTeamServiceClient.fetchMembers(eq(1L), any(), any()))
                .thenReturn(List.of(remoteMember(1L, 5L)));
        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/assign")).contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(5L)))
                .andExpect(status().isOk());

        mockMvc.perform(asUser(patch(TASKS_URL + "/" + id + "/status"), 5L).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PROGRESS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void nonAssignedUserCannotUpdateTaskStatus() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Assigned to someone else status task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        when(projectTeamServiceClient.fetchMembers(eq(1L), any(), any()))
                .thenReturn(List.of(remoteMember(1L, 5L)));
        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/assign")).contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(5L)))
                .andExpect(status().isOk());

        mockMvc.perform(asUser(patch(TASKS_URL + "/" + id + "/status"), 6L).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PROGRESS")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access is denied."));
    }

    @Test
    void userCannotUpdateStatusOnUnassignedTask() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Unassigned status task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asUser(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PROGRESS")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access is denied."));
    }

    // --- progress updates ---

    @Test
    void updateProgressWhenInProgressReturnsOk() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Progress task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PROGRESS")))
                .andExpect(status().isOk());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/progress")).contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(45)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercentage").value(45));
    }

    @Test
    void updateProgressWhenTodoReturnsBadRequest() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Todo progress task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/progress")).contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(45)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateProgressWhenCompletedReturnsBadRequest() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Completed progress task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("COMPLETED")))
                .andExpect(status().isOk());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/progress")).contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(45)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProgressAboveMaximumReturnsBadRequest() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Over-progress task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PROGRESS")))
                .andExpect(status().isOk());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/progress")).contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(101)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProgressBelowMinimumReturnsBadRequest() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Under-progress task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PROGRESS")))
                .andExpect(status().isOk());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/progress")).contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(-1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProgressMissingTaskReturnsNotFound() throws Exception {
        mockMvc.perform(asManager(patch(TASKS_URL + "/999999/progress")).contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(45)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userRoleCannotUpdateTaskProgress() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Protected progress task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PROGRESS")))
                .andExpect(status().isOk());

        mockMvc.perform(asUser(patch(TASKS_URL + "/" + id + "/progress")).contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(45)))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignedUserCanUpdateOwnTaskProgress() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Assigned progress task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        when(projectTeamServiceClient.fetchMembers(eq(1L), any(), any()))
                .thenReturn(List.of(remoteMember(1L, 5L)));
        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/assign")).contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(5L)))
                .andExpect(status().isOk());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PROGRESS")))
                .andExpect(status().isOk());

        mockMvc.perform(asUser(patch(TASKS_URL + "/" + id + "/progress"), 5L).contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(45)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercentage").value(45));
    }

    @Test
    void nonAssignedUserCannotUpdateTaskProgress() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Assigned to someone else progress task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        when(projectTeamServiceClient.fetchMembers(eq(1L), any(), any()))
                .thenReturn(List.of(remoteMember(1L, 5L)));
        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/assign")).contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody(5L)))
                .andExpect(status().isOk());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PROGRESS")))
                .andExpect(status().isOk());

        mockMvc.perform(asUser(patch(TASKS_URL + "/" + id + "/progress"), 6L).contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(45)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access is denied."));
    }

    @Test
    void userCannotUpdateProgressOnUnassignedTask() throws Exception {
        String response = mockMvc.perform(asManager(post(TASKS_URL)).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(1L, "Unassigned progress task", null, "LOW", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(com.jayway.jsonpath.JsonPath.read(response, "$.id").toString());

        mockMvc.perform(asManager(patch(TASKS_URL + "/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content(statusBody("IN_PROGRESS")))
                .andExpect(status().isOk());

        mockMvc.perform(asUser(patch(TASKS_URL + "/" + id + "/progress")).contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(45)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access is denied."));
    }
}
