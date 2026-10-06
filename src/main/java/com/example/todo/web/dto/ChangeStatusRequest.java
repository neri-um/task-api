package com.example.todo.web.dto;

import com.example.todo.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
    @NotNull(message = "el estado es obligatorio") TaskStatus status) {}
