package com.example.todo.service;

import com.example.todo.domain.Task;
import com.example.todo.domain.TaskPriority;
import com.example.todo.domain.TaskStats;
import com.example.todo.domain.TaskStatus;
import com.example.todo.exception.InvalidStatusTransitionException;
import com.example.todo.exception.TaskNotFoundException;
import com.example.todo.exception.TaskStateException;
import com.example.todo.exception.TaskValidationException;
import com.example.todo.repository.TaskRepository;
import com.example.todo.web.dto.CreateTaskRequest;
import com.example.todo.web.dto.UpdateTaskRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

  private final TaskRepository repository;
  private final Clock clock;

  public TaskService(TaskRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  public Task create(CreateTaskRequest request) {
    validateDueDate(request.dueDate());
    validateTags(request.tags());

    Instant now = Instant.now(clock);
    Task task = new Task();
    task.setTitle(request.title().trim());
    task.setDescription(trimToNull(request.description()));
    task.setPriority(request.priority());
    task.setStatus(TaskStatus.TODO);
    task.setDueDate(request.dueDate());
    task.setAssignee(trimToNull(request.assignee()));
    task.setTags(normalizeTags(request.tags()));
    task.setCreatedAt(now);
    task.setUpdatedAt(now);
    task.setCompletedAt(null);
    return repository.save(task);
  }

  public Task get(Long id) {
    return repository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
  }

  public List<Task> list(TaskStatus status, TaskPriority priority, String tag) {
    return repository.findAll().stream()
        .filter(task -> status == null || task.getStatus() == status)
        .filter(task -> priority == null || task.getPriority() == priority)
        .filter(task -> tag == null || matchesTag(task, tag))
        .toList();
  }

  public Task update(Long id, UpdateTaskRequest request) {
    Task task = get(id);
    ensureNotFinished(task);
    validateDueDate(request.dueDate());
    validateTags(request.tags());

    task.setTitle(request.title().trim());
    task.setDescription(trimToNull(request.description()));
    task.setPriority(request.priority());
    task.setDueDate(request.dueDate());
    task.setAssignee(trimToNull(request.assignee()));
    task.setTags(normalizeTags(request.tags()));
    task.setUpdatedAt(Instant.now(clock));
    return repository.save(task);
  }

  public Task changeStatus(Long id, TaskStatus target) {
    Task task = get(id);
    TaskStatus current = task.getStatus();

    if (current == target) {
      return task;
    }
    if (!current.canTransitionTo(target)) {
      throw new InvalidStatusTransitionException(current, target);
    }

    Instant now = Instant.now(clock);
    task.setStatus(target);
    task.setUpdatedAt(now);
    task.setCompletedAt(target == TaskStatus.DONE ? now : null);
    return repository.save(task);
  }

  public void delete(Long id) {
    repository.delete(get(id));
  }

  public List<Task> findOverdue() {
    LocalDate today = LocalDate.now(clock);
    return repository.findAll().stream()
        .filter(task -> !task.getStatus().isFinished())
        .filter(task -> task.getDueDate().isBefore(today))
        .toList();
  }

  public TaskStats stats() {
    List<Task> all = repository.findAll();
    Map<TaskStatus, Long> byStatus =
        all.stream().collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
    return new TaskStats(all.size(), Map.copyOf(byStatus));
  }

  private void ensureNotFinished(Task task) {
    if (task.getStatus().isFinished()) {
      throw new TaskStateException(
          "La tarea " + task.getId() + " esta " + task.getStatus() + " y ya no puede modificarse");
    }
  }

  private void validateDueDate(LocalDate dueDate) {
    LocalDate today = LocalDate.now(clock);
    if (dueDate.isBefore(today)) {
      throw new TaskValidationException(
          "La fecha limite no puede ser anterior a hoy (" + today + "): " + dueDate);
    }
  }

  private void validateTags(Set<String> tags) {
    if (tags == null) {
      return;
    }
    Set<String> seen = new LinkedHashSet<>();
    for (String rawTag : tags) {
      String tag = rawTag == null ? "" : rawTag.trim();
      if (tag.isEmpty()) {
        throw new TaskValidationException("Las etiquetas no pueden estar en blanco");
      }
      if (!seen.add(tag.toLowerCase(Locale.ROOT))) {
        throw new TaskValidationException("Etiqueta duplicada: " + tag);
      }
    }
  }

  private Set<String> normalizeTags(Set<String> tags) {
    if (tags == null) {
      return new LinkedHashSet<>();
    }
    Set<String> normalized = new LinkedHashSet<>();
    tags.forEach(tag -> normalized.add(tag.trim()));
    return normalized;
  }

  private boolean matchesTag(Task task, String tag) {
    return task.getTags().stream().anyMatch(candidate -> candidate.equalsIgnoreCase(tag.trim()));
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
