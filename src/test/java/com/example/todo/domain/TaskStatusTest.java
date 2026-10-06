package com.example.todo.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TaskStatusTest {

  @Test
  void soloSePuedeAvanzarDesdeTodoAHacerseEnCursoOCancelarse() {
    assertTrue(TaskStatus.TODO.canTransitionTo(TaskStatus.IN_PROGRESS));
    assertTrue(TaskStatus.TODO.canTransitionTo(TaskStatus.CANCELLED));
    assertFalse(TaskStatus.TODO.canTransitionTo(TaskStatus.DONE));
    assertFalse(TaskStatus.TODO.canTransitionTo(TaskStatus.BLOCKED));
  }

  @Test
  void lasEstadosFinalizadosNoPermitenSalidas() {
    assertFalse(TaskStatus.DONE.canTransitionTo(TaskStatus.IN_PROGRESS));
    assertFalse(TaskStatus.DONE.canTransitionTo(TaskStatus.CANCELLED));
    assertFalse(TaskStatus.CANCELLED.canTransitionTo(TaskStatus.IN_PROGRESS));
    assertFalse(TaskStatus.CANCELLED.canTransitionTo(TaskStatus.DONE));
    assertTrue(TaskStatus.DONE.isFinished());
    assertTrue(TaskStatus.CANCELLED.isFinished());
    assertFalse(TaskStatus.BLOCKED.isFinished());
  }

  @Test
  void ningunaTransicionAUnEstadoIgualAlActual() {
    for (TaskStatus status : TaskStatus.values()) {
      assertFalse(status.canTransitionTo(status));
    }
  }
}
