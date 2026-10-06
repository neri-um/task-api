package com.example.todo.web.dto;

import com.example.todo.domain.TaskPriority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

public record UpdateTaskRequest(
    @NotBlank(message = "el titulo no puede estar en blanco")
        @Size(min = 3, max = 100, message = "el titulo debe tener entre 3 y 100 caracteres")
        String title,
    @Size(max = 2000, message = "la descripcion no puede superar los 2000 caracteres")
        String description,
    @NotNull(message = "la prioridad es obligatoria") TaskPriority priority,
    @NotNull(message = "la fecha limite es obligatoria") LocalDate dueDate,
    @Email(message = "el asignatario debe ser un email valido")
        @Size(max = 255, message = "el asignatario no puede superar los 255 caracteres")
        String assignee,
    @Size(max = 10, message = "como maximo se admiten 10 etiquetas")
        Set<@NotBlank @Size(max = 20) String> tags) {}
