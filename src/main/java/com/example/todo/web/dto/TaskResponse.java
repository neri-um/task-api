package com.example.todo.web.dto;

import com.example.todo.domain.Task;
import com.example.todo.domain.TaskPriority;
import com.example.todo.domain.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record TaskResponse(
    Long id,
    String title,
    String description,
    TaskPriority priority,
    TaskStatus status,
    LocalDate dueDate,
    String assignee,
    Set<String> tags,
    Instant createdAt,
    Instant updatedAt,
    Instant completedAt) {

  public static TaskResponse from(Task task) {
    return new TaskResponse(
        task.getId(),
        task.getTitle(),
        task.getDescription(),
        task.getPriority(),
        task.getStatus(),
        task.getDueDate(),
        task.getAssignee(),
        Set.copyOf(task.getTags()),
        task.getCreatedAt(),
        task.getUpdatedAt(),
        task.getCompletedAt());
  }

  public static List<TaskResponse> from(List<Task> tasks) {
    return tasks.stream().map(TaskResponse::from).toList();
  }
}
