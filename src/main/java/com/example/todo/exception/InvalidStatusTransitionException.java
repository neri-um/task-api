package com.example.todo.exception;

import com.example.todo.domain.TaskStatus;

public class InvalidStatusTransitionException extends RuntimeException {

  public InvalidStatusTransitionException(TaskStatus from, TaskStatus to) {
    super("Transicion de estado no permitida: " + from + " -> " + to);
  }
}
