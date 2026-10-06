package com.example.todo.repository;

import com.example.todo.domain.Task;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTaskRepository implements TaskRepository {

  private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
  private final AtomicLong sequence = new AtomicLong();

  @Override
  public Task save(Task task) {
    if (task.getId() == null) {
      task.setId(sequence.incrementAndGet());
    }
    tasks.put(task.getId(), task);
    return task;
  }

  @Override
  public Optional<Task> findById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(tasks.get(id));
  }

  @Override
  public List<Task> findAll() {
    List<Task> all = new ArrayList<>(tasks.values());
    all.sort(Comparator.comparing(Task::getId));
    return all;
  }

  @Override
  public void delete(Task task) {
    tasks.remove(task.getId());
  }

  @Override
  public long count() {
    return tasks.size();
  }
}
