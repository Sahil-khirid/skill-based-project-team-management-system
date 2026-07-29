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
import com.skillteam.taskprogress.entity.TaskStatus;
import com.skillteam.taskprogress.exception.ForbiddenException;
import com.skillteam.taskprogress.exception.InvalidTaskAssignmentException;
import com.skillteam.taskprogress.exception.InvalidTaskProgressException;
import com.skillteam.taskprogress.exception.InvalidTaskStatusTransitionException;
import com.skillteam.taskprogress.exception.TaskNotFoundException;
import com.skillteam.taskprogress.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TaskService {

    private static final String NOT_FOUND_MESSAGE = "No task exists for this id.";

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
            TaskStatus.TODO, Set.of(TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, TaskStatus.BLOCKED),
            TaskStatus.IN_PROGRESS, Set.of(TaskStatus.COMPLETED, TaskStatus.BLOCKED),
            TaskStatus.BLOCKED, Set.of(TaskStatus.IN_PROGRESS),
            TaskStatus.COMPLETED, Set.of()
    );

    private static final String NOT_PROJECT_MEMBER_MESSAGE =
            "Cannot assign this task: the user is not a member of this task's project.";

    private static final String ROLE_PROJECT_MANAGER = "PROJECT_MANAGER";
    private static final String ROLE_USER = "USER";
    private static final String FORBIDDEN_MESSAGE = "Access is denied.";

    private final TaskRepository taskRepository;
    private final ProjectTeamServiceClient projectTeamServiceClient;

    public TaskService(TaskRepository taskRepository, ProjectTeamServiceClient projectTeamServiceClient) {
        this.taskRepository = taskRepository;
        this.projectTeamServiceClient = projectTeamServiceClient;
    }

    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        Task task = new Task(request.projectId(), request.title(), request.description(),
                TaskStatus.TODO, request.priority(), request.dueDate());

        Task saved = taskRepository.saveAndFlush(task);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list() {
        return taskRepository.findAllByOrderByIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        Task task = findOrThrow(id);
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public TaskProgressSummaryResponse getProgressSummary(Long projectId) {
        long total = taskRepository.countByProjectId(projectId);
        long todo = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.TODO);
        long inProgress = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.IN_PROGRESS);
        long completed = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.COMPLETED);
        long blocked = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.BLOCKED);

        double completionPercentage = total == 0 ? 0.0 : roundToTwoDecimals(completed * 100.0 / total);

        return new TaskProgressSummaryResponse(projectId, total, todo, inProgress, completed, blocked,
                completionPercentage);
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = findOrThrow(id);

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());

        Task saved = taskRepository.saveAndFlush(task);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Task task = findOrThrow(id);
        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponse assign(Long id, AssignTaskRequest request, Long callerUserId, String callerRole) {
        Task task = findOrThrow(id);

        List<RemoteProjectMemberResponse> members =
                projectTeamServiceClient.fetchMembers(task.getProjectId(), callerUserId, callerRole);
        boolean isMember = members.stream()
                .anyMatch(member -> member.authUserId().equals(request.assignedAuthUserId()));
        if (!isMember) {
            throw new InvalidTaskAssignmentException(NOT_PROJECT_MEMBER_MESSAGE);
        }

        task.setAssignedAuthUserId(request.assignedAuthUserId());

        Task saved = taskRepository.saveAndFlush(task);
        return toResponse(saved);
    }

    @Transactional
    public TaskResponse updateStatus(Long id, UpdateTaskStatusRequest request, Long callerUserId, String callerRole) {
        Task task = findOrThrow(id);
        requireManagerOrAssignee(task, callerUserId, callerRole);

        TaskStatus current = task.getStatus();
        TaskStatus target = request.status();

        if (!ALLOWED_STATUS_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new InvalidTaskStatusTransitionException(
                    "Cannot transition task status from " + current + " to " + target + ".");
        }

        task.setStatus(target);
        if (target == TaskStatus.COMPLETED) {
            task.setProgressPercentage(100);
        }

        Task saved = taskRepository.saveAndFlush(task);
        return toResponse(saved);
    }

    @Transactional
    public TaskResponse updateProgress(Long id, UpdateTaskProgressRequest request, Long callerUserId, String callerRole) {
        Task task = findOrThrow(id);
        requireManagerOrAssignee(task, callerUserId, callerRole);

        TaskStatus status = task.getStatus();

        if (status == TaskStatus.TODO) {
            throw new InvalidTaskProgressException("Cannot update progress while task status is TODO.");
        }
        if (status == TaskStatus.COMPLETED) {
            throw new InvalidTaskProgressException("Cannot update progress while task status is COMPLETED.");
        }

        task.setProgressPercentage(request.progressPercentage());

        Task saved = taskRepository.saveAndFlush(task);
        return toResponse(saved);
    }

    private Task findOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(NOT_FOUND_MESSAGE));
    }

    private void requireManagerOrAssignee(Task task, Long callerUserId, String callerRole) {
        if (ROLE_PROJECT_MANAGER.equals(callerRole)) {
            return;
        }

        if (ROLE_USER.equals(callerRole)
                && task.getAssignedAuthUserId() != null
                && task.getAssignedAuthUserId().equals(callerUserId)) {
            return;
        }

        throw new ForbiddenException(FORBIDDEN_MESSAGE);
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProjectId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getAssignedAuthUserId(),
                task.getProgressPercentage(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
