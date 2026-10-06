package com.example.todo.exception;

public class TaskValidationException extends RuntimeException {

  public TaskValidationException(String message) {
    super(message);
  }
}
