package com.skillteam.taskprogress.service;

import com.skillteam.taskprogress.client.ProjectTeamServiceClient;
import com.skillteam.taskprogress.client.RemoteProjectMemberResponse;
import com.skillteam.taskprogress.dto.AssignTaskRequest;
import com.skillteam.taskprogress.dto.CreateTaskRequest;
import com.skillteam.taskprogress.dto.TaskProgressSummaryResponse;
import com.skillteam.taskprogress.dto.TaskResponse;
import com.skillteam.taskprogress.dto.UpdateTaskProgressRequest;
import com.skillteam.taskprogress.dto.UpdateTaskRequest;
import com.skillteam.taskprogress.dto.UpdateTaskStatusRequest;
import com.skillteam.taskprogress.entity.Task;
import com.skillteam.taskprogress.entity.TaskPriority;
import com.skillteam.taskprogress.entity.TaskStatus;
import com.skillteam.taskprogress.exception.ForbiddenException;
import com.skillteam.taskprogress.exception.InvalidTaskAssignmentException;
import com.skillteam.taskprogress.exception.InvalidTaskProgressException;
import com.skillteam.taskprogress.exception.InvalidTaskStatusTransitionException;
import com.skillteam.taskprogress.exception.ProjectTeamServiceUnavailableException;
import com.skillteam.taskprogress.exception.TaskNotFoundException;
import com.skillteam.taskprogress.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long CALLER_USER_ID = 1L;
    private static final String CALLER_ROLE = "PROJECT_MANAGER";
    private static final String USER_ROLE = "USER";
    private static final Long ASSIGNEE_USER_ID = 42L;
    private static final Long OTHER_USER_ID = 99L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectTeamServiceClient projectTeamServiceClient;

    @InjectMocks
    private TaskService taskService;

    private RemoteProjectMemberResponse member(Long projectId, Long authUserId) {
        return new RemoteProjectMemberResponse(authUserId, projectId, authUserId, "MEMBER", Instant.now(), Instant.now());
    }

    @Test
    void createAlwaysStartsANewTaskInTodoStatus() {
        CreateTaskRequest request = new CreateTaskRequest(1L, "Design schema", null, TaskPriority.HIGH, null);

        when(taskRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.create(request);

        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.projectId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Design schema");
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void getMissingTaskIsTranslatedToTaskNotFoundException() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.get(999L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("No task exists for this id.");
    }

    @Test
    void updateMissingTaskIsTranslatedToTaskNotFoundException() {
        UpdateTaskRequest request =
                new UpdateTaskRequest("Renamed", null, TaskStatus.IN_PROGRESS, TaskPriority.LOW, null);
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(999L, request))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("No task exists for this id.");
    }

    @Test
    void updateAppliesAllEditableFieldsToTheExistingTask() {
        Task task = new Task(1L, "Old title", "Old description", TaskStatus.TODO, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate dueDate = LocalDate.of(2026, 8, 1);
        UpdateTaskRequest request =
                new UpdateTaskRequest("New title", "New description", TaskStatus.COMPLETED, TaskPriority.HIGH, dueDate);

        TaskResponse response = taskService.update(5L, request);

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.description()).isEqualTo("New description");
        assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.dueDate()).isEqualTo(dueDate);
    }

    @Test
    void deleteMissingTaskIsTranslatedToTaskNotFoundException() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.delete(999L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("No task exists for this id.");
    }

    @Test
    void deleteRemovesTheExistingTask() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.TODO, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        taskService.delete(5L);

        verify(taskRepository).delete(task);
    }

    // --- assignment ---

    @Test
    void assignSetsAssignedAuthUserIdWhenTargetUserIsAProjectMember() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.TODO, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(projectTeamServiceClient.fetchMembers(1L, CALLER_USER_ID, CALLER_ROLE))
                .thenReturn(List.of(member(1L, 9L)));
        when(taskRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.assign(5L, new AssignTaskRequest(9L), CALLER_USER_ID, CALLER_ROLE);

        assertThat(response.assignedAuthUserId()).isEqualTo(9L);
    }

    @Test
    void assignMissingTaskIsTranslatedToTaskNotFoundException() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.assign(999L, new AssignTaskRequest(9L), CALLER_USER_ID, CALLER_ROLE))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("No task exists for this id.");
    }

    @Test
    void assignToUserWhoIsNotAProjectMemberThrowsInvalidTaskAssignmentException() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.TODO, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(projectTeamServiceClient.fetchMembers(1L, CALLER_USER_ID, CALLER_ROLE))
                .thenReturn(List.of(member(1L, 20L)));

        assertThatThrownBy(() -> taskService.assign(5L, new AssignTaskRequest(9L), CALLER_USER_ID, CALLER_ROLE))
                .isInstanceOf(InvalidTaskAssignmentException.class);
    }

    @Test
    void assignWhenProjectHasNoMembersThrowsInvalidTaskAssignmentException() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.TODO, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(projectTeamServiceClient.fetchMembers(1L, CALLER_USER_ID, CALLER_ROLE)).thenReturn(List.of());

        assertThatThrownBy(() -> taskService.assign(5L, new AssignTaskRequest(9L), CALLER_USER_ID, CALLER_ROLE))
                .isInstanceOf(InvalidTaskAssignmentException.class);
    }

    @Test
    void assignPropagatesProjectTeamServiceUnavailableExceptionOnDownstreamFailure() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.TODO, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(projectTeamServiceClient.fetchMembers(1L, CALLER_USER_ID, CALLER_ROLE))
                .thenThrow(new ProjectTeamServiceUnavailableException("downstream unavailable", new RuntimeException()));

        assertThatThrownBy(() -> taskService.assign(5L, new AssignTaskRequest(9L), CALLER_USER_ID, CALLER_ROLE))
                .isInstanceOf(ProjectTeamServiceUnavailableException.class);
    }

    // --- status transitions ---

    @Test
    void updateStatusFromTodoToInProgressSucceeds() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.TODO, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateStatus(5L, new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS),
                CALLER_USER_ID, CALLER_ROLE);

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateStatusToCompletedForcesProgressToOneHundred() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.IN_PROGRESS, TaskPriority.LOW, null);
        task.setProgressPercentage(40);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateStatus(5L, new UpdateTaskStatusRequest(TaskStatus.COMPLETED),
                CALLER_USER_ID, CALLER_ROLE);

        assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.progressPercentage()).isEqualTo(100);
    }

    @Test
    void updateStatusFromCompletedToAnyStatusIsRejected() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.COMPLETED, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(5L, new UpdateTaskStatusRequest(TaskStatus.TODO),
                CALLER_USER_ID, CALLER_ROLE))
                .isInstanceOf(InvalidTaskStatusTransitionException.class);
        assertThatThrownBy(() -> taskService.updateStatus(5L, new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS),
                CALLER_USER_ID, CALLER_ROLE))
                .isInstanceOf(InvalidTaskStatusTransitionException.class);
        assertThatThrownBy(() -> taskService.updateStatus(5L, new UpdateTaskStatusRequest(TaskStatus.BLOCKED),
                CALLER_USER_ID, CALLER_ROLE))
                .isInstanceOf(InvalidTaskStatusTransitionException.class);
    }

    @Test
    void updateStatusFromTodoToBlockedSucceeds() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.TODO, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateStatus(5L, new UpdateTaskStatusRequest(TaskStatus.BLOCKED),
                CALLER_USER_ID, CALLER_ROLE);

        assertThat(response.status()).isEqualTo(TaskStatus.BLOCKED);
    }

    @Test
    void updateStatusFromBlockedToInProgressSucceeds() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.BLOCKED, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateStatus(5L, new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS),
                CALLER_USER_ID, CALLER_ROLE);

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateStatusFromBlockedToCompletedIsRejected() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.BLOCKED, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(5L, new UpdateTaskStatusRequest(TaskStatus.COMPLETED),
                CALLER_USER_ID, CALLER_ROLE))
                .isInstanceOf(InvalidTaskStatusTransitionException.class);
    }

    @Test
    void updateStatusMissingTaskIsTranslatedToTaskNotFoundException() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateStatus(999L, new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS),
                CALLER_USER_ID, CALLER_ROLE))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("No task exists for this id.");
    }

    // --- status/progress authorization: PROJECT_MANAGER, assignee, others ---

    @Test
    void projectManagerCanUpdateStatusOnAnyTask() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.TODO, TaskPriority.LOW, null);
        task.setAssignedAuthUserId(ASSIGNEE_USER_ID);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateStatus(5L, new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS),
                CALLER_USER_ID, CALLER_ROLE);

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void projectManagerCanUpdateProgressOnAnyTask() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.IN_PROGRESS, TaskPriority.LOW, null);
        task.setAssignedAuthUserId(ASSIGNEE_USER_ID);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateProgress(5L, new UpdateTaskProgressRequest(60),
                CALLER_USER_ID, CALLER_ROLE);

        assertThat(response.progressPercentage()).isEqualTo(60);
    }

    @Test
    void assignedUserCanUpdateOwnTaskStatus() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.TODO, TaskPriority.LOW, null);
        task.setAssignedAuthUserId(ASSIGNEE_USER_ID);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateStatus(5L, new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS),
                ASSIGNEE_USER_ID, USER_ROLE);

        assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void assignedUserCanUpdateOwnTaskProgress() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.IN_PROGRESS, TaskPriority.LOW, null);
        task.setAssignedAuthUserId(ASSIGNEE_USER_ID);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateProgress(5L, new UpdateTaskProgressRequest(70),
                ASSIGNEE_USER_ID, USER_ROLE);

        assertThat(response.progressPercentage()).isEqualTo(70);
    }

    @Test
    void nonAssignedUserCannotUpdateTaskStatus() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.TODO, TaskPriority.LOW, null);
        task.setAssignedAuthUserId(ASSIGNEE_USER_ID);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(5L, new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS),
                OTHER_USER_ID, USER_ROLE))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access is denied.");
    }

    @Test
    void nonAssignedUserCannotUpdateTaskProgress() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.IN_PROGRESS, TaskPriority.LOW, null);
        task.setAssignedAuthUserId(ASSIGNEE_USER_ID);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateProgress(5L, new UpdateTaskProgressRequest(50),
                OTHER_USER_ID, USER_ROLE))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access is denied.");
    }

    @Test
    void userCannotUpdateStatusOnUnassignedTask() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.TODO, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(5L, new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS),
                OTHER_USER_ID, USER_ROLE))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access is denied.");
    }

    @Test
    void userCannotUpdateProgressOnUnassignedTask() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.IN_PROGRESS, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateProgress(5L, new UpdateTaskProgressRequest(50),
                OTHER_USER_ID, USER_ROLE))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access is denied.");
    }

    // --- progress updates ---

    @Test
    void updateProgressSucceedsWhenTaskIsInProgress() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.IN_PROGRESS, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateProgress(5L, new UpdateTaskProgressRequest(45),
                CALLER_USER_ID, CALLER_ROLE);

        assertThat(response.progressPercentage()).isEqualTo(45);
    }

    @Test
    void updateProgressSucceedsWhenTaskIsBlocked() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.BLOCKED, TaskPriority.LOW, null);
        task.setProgressPercentage(20);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));
        when(taskRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateProgress(5L, new UpdateTaskProgressRequest(30),
                CALLER_USER_ID, CALLER_ROLE);

        assertThat(response.progressPercentage()).isEqualTo(30);
    }

    @Test
    void updateProgressFailsWhenTaskIsTodo() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.TODO, TaskPriority.LOW, null);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateProgress(5L, new UpdateTaskProgressRequest(50),
                CALLER_USER_ID, CALLER_ROLE))
                .isInstanceOf(InvalidTaskProgressException.class);
    }

    @Test
    void updateProgressFailsWhenTaskIsCompleted() {
        Task task = new Task(1L, "Apollo", null, TaskStatus.COMPLETED, TaskPriority.LOW, null);
        task.setProgressPercentage(100);
        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateProgress(5L, new UpdateTaskProgressRequest(50),
                CALLER_USER_ID, CALLER_ROLE))
                .isInstanceOf(InvalidTaskProgressException.class);
    }

    @Test
    void updateProgressMissingTaskIsTranslatedToTaskNotFoundException() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateProgress(999L, new UpdateTaskProgressRequest(50),
                CALLER_USER_ID, CALLER_ROLE))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("No task exists for this id.");
    }

    // --- progress summary ---

    @Test
    void progressSummaryForProjectWithNoTasksReturnsAllZeroesWithoutDivisionByZero() {
        when(taskRepository.countByProjectId(1L)).thenReturn(0L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.TODO)).thenReturn(0L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.IN_PROGRESS)).thenReturn(0L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.COMPLETED)).thenReturn(0L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.BLOCKED)).thenReturn(0L);

        TaskProgressSummaryResponse summary = taskService.getProgressSummary(1L);

        assertThat(summary.projectId()).isEqualTo(1L);
        assertThat(summary.totalTasks()).isZero();
        assertThat(summary.todoCount()).isZero();
        assertThat(summary.inProgressCount()).isZero();
        assertThat(summary.completedCount()).isZero();
        assertThat(summary.blockedCount()).isZero();
        assertThat(summary.completionPercentage()).isEqualTo(0.0);
    }

    @Test
    void progressSummaryForProjectWithMixedStatusesComputesCorrectCounts() {
        when(taskRepository.countByProjectId(1L)).thenReturn(10L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.TODO)).thenReturn(3L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.IN_PROGRESS)).thenReturn(2L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.COMPLETED)).thenReturn(4L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.BLOCKED)).thenReturn(1L);

        TaskProgressSummaryResponse summary = taskService.getProgressSummary(1L);

        assertThat(summary.totalTasks()).isEqualTo(10L);
        assertThat(summary.todoCount()).isEqualTo(3L);
        assertThat(summary.inProgressCount()).isEqualTo(2L);
        assertThat(summary.completedCount()).isEqualTo(4L);
        assertThat(summary.blockedCount()).isEqualTo(1L);
        assertThat(summary.completionPercentage()).isEqualTo(40.0);
    }

    @Test
    void progressSummaryRoundsCompletionPercentageToTwoDecimals() {
        when(taskRepository.countByProjectId(1L)).thenReturn(3L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.TODO)).thenReturn(2L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.IN_PROGRESS)).thenReturn(0L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.COMPLETED)).thenReturn(1L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.BLOCKED)).thenReturn(0L);

        TaskProgressSummaryResponse summary = taskService.getProgressSummary(1L);

        assertThat(summary.completionPercentage()).isEqualTo(33.33);
    }

    @Test
    void progressSummaryForFullyCompletedProjectReturnsOneHundredPercent() {
        when(taskRepository.countByProjectId(1L)).thenReturn(5L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.TODO)).thenReturn(0L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.IN_PROGRESS)).thenReturn(0L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.COMPLETED)).thenReturn(5L);
        when(taskRepository.countByProjectIdAndStatus(1L, TaskStatus.BLOCKED)).thenReturn(0L);

        TaskProgressSummaryResponse summary = taskService.getProgressSummary(1L);

        assertThat(summary.completionPercentage()).isEqualTo(100.0);
    }
}
