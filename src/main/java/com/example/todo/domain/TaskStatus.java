package com.example.todo.domain;

public enum TaskStatus {
  TODO,
  IN_PROGRESS,
  BLOCKED,
  DONE,
  CANCELLED;

  public boolean isFinished() {
    return this == DONE || this == CANCELLED;
  }

  public boolean canTransitionTo(TaskStatus target) {
    if (target == null || target == this) {
      return false;
    }
    return switch (this) {
      case TODO -> target == IN_PROGRESS || target == CANCELLED;
      case IN_PROGRESS -> target == BLOCKED || target == DONE || target == CANCELLED;
      case BLOCKED -> target == IN_PROGRESS || target == CANCELLED;
      case DONE, CANCELLED -> false;
    };
  }
}
