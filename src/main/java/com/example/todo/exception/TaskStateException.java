package com.example.todo.exception;

public class TaskStateException extends RuntimeException {

  public TaskStateException(String message) {
    super(message);
  }
}
