package com.example.todo.repository;

import com.example.todo.domain.Task;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {

  Task save(Task task);

  Optional<Task> findById(Long id);

  List<Task> findAll();

  void delete(Task task);

  long count();
}
