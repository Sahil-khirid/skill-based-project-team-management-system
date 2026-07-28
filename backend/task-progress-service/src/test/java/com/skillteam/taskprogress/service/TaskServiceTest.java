package com.skillteam.taskprogress.service;

import com.skillteam.taskprogress.dto.CreateTaskRequest;
import com.skillteam.taskprogress.dto.TaskResponse;
import com.skillteam.taskprogress.dto.UpdateTaskRequest;
import com.skillteam.taskprogress.entity.Task;
import com.skillteam.taskprogress.entity.TaskPriority;
import com.skillteam.taskprogress.entity.TaskStatus;
import com.skillteam.taskprogress.exception.TaskNotFoundException;
import com.skillteam.taskprogress.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

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
}
