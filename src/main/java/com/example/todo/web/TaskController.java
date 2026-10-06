package com.example.todo.web;

import com.example.todo.domain.TaskPriority;
import com.example.todo.domain.TaskStats;
import com.example.todo.domain.TaskStatus;
import com.example.todo.service.TaskService;
import com.example.todo.web.dto.ChangeStatusRequest;
import com.example.todo.web.dto.CreateTaskRequest;
import com.example.todo.web.dto.TaskResponse;
import com.example.todo.web.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @PostMapping
  public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
    TaskResponse created = TaskResponse.from(taskService.create(request));
    return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
  }

  @GetMapping
  public List<TaskResponse> list(
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false) TaskPriority priority,
      @RequestParam(required = false) String tag) {
    return TaskResponse.from(taskService.list(status, priority, tag));
  }

  @GetMapping("/stats")
  public TaskStats stats() {
    return taskService.stats();
  }

  @GetMapping("/overdue")
  public List<TaskResponse> overdue() {
    return TaskResponse.from(taskService.findOverdue());
  }

  @GetMapping("/{id}")
  public TaskResponse get(@PathVariable Long id) {
    return TaskResponse.from(taskService.get(id));
  }

  @PutMapping("/{id}")
  public TaskResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
    return TaskResponse.from(taskService.update(id, request));
  }

  @PutMapping("/{id}/status")
  public TaskResponse changeStatus(
      @PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request) {
    return TaskResponse.from(taskService.changeStatus(id, request.status()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    taskService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
