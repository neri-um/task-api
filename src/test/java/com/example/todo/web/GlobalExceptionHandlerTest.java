package com.example.todo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.todo.domain.TaskPriority;
import com.example.todo.domain.TaskStatus;
import com.example.todo.exception.InvalidStatusTransitionException;
import com.example.todo.exception.TaskNotFoundException;
import com.example.todo.exception.TaskStateException;
import com.example.todo.exception.TaskValidationException;
import com.example.todo.web.dto.CreateTaskRequest;
import com.example.todo.web.dto.ErrorResponse;
import java.lang.reflect.Method;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler handler;
  private MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    handler = new GlobalExceptionHandler();
    request = new MockHttpServletRequest("GET", "/api/tasks/42");
  }

  @Test
  void unaTareaInexistenteResponde404SinTrazaDeExcepcion() {
    ResponseEntity<ErrorResponse> response =
        handler.handleNotFound(new TaskNotFoundException(42L), request);

    assertEquals(404, response.getStatusCode().value());
    assertEquals("/api/tasks/42", response.getBody().path());
    assertTrue(response.getBody().message().contains("42"));
    assertEquals("Not Found", response.getBody().error());
  }

  @Test
  void unaReglaDeNegocioVioladaResponde400() {
    ResponseEntity<ErrorResponse> response =
        handler.handleBusinessValidation(
            new TaskValidationException("La fecha limite no puede ser anterior a hoy"), request);

    assertEquals(400, response.getStatusCode().value());
    assertTrue(response.getBody().message().contains("fecha limite"));
  }

  @Test
  void unaTransicionDeEstadoInvalidaResponde409() {
    ResponseEntity<ErrorResponse> response =
        handler.handleInvalidTransition(
            new InvalidStatusTransitionException(TaskStatus.TODO, TaskStatus.DONE), request);

    assertEquals(409, response.getStatusCode().value());
    assertTrue(response.getBody().message().contains("TODO -> DONE"));
  }

  @Test
  void modificarUnaTareaFinalizadaResponde409() {
    ResponseEntity<ErrorResponse> response =
        handler.handleConflict(
            new TaskStateException("La tarea 1 esta DONE y ya no puede modificarse"), request);

    assertEquals(409, response.getStatusCode().value());
    assertTrue(response.getBody().message().contains("DONE"));
  }

  @Test
  void erroresDeValidacionDeBeanValidationIncluyenLosCampos() throws NoSuchMethodException {
    CreateTaskRequest invalid =
        new CreateTaskRequest(null, null, null, LocalDate.now(), "no-es-un-email", null);
    BindException bindingResult = new BindException(invalid, "createTaskRequest");
    bindingResult.addError(
        new FieldError("createTaskRequest", "title", "el titulo no puede estar en blanco"));
    bindingResult.addError(
        new FieldError("createTaskRequest", "priority", "la prioridad es obligatoria"));
    MethodParameter parameter =
        new MethodParameter(
            TaskController.class.getDeclaredMethod("create", CreateTaskRequest.class), 0);
    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(parameter, bindingResult.getBindingResult());

    ResponseEntity<ErrorResponse> response = handler.handleBeanValidation(exception, request);

    assertEquals(400, response.getStatusCode().value());
    assertEquals(
        "el titulo no puede estar en blanco", response.getBody().fieldErrors().get("title"));
    assertEquals("la prioridad es obligatoria", response.getBody().fieldErrors().get("priority"));
  }

  @Test
  void unParametroConValorInvalidoResponde400() throws NoSuchMethodException {
    Method list =
        TaskController.class.getDeclaredMethod(
            "list", TaskStatus.class, TaskPriority.class, String.class);
    MethodParameter methodParameter = new MethodParameter(list, 0);
    MethodArgumentTypeMismatchException exception =
        new MethodArgumentTypeMismatchException(
            "NO_EXISTE", TaskStatus.class, "status", methodParameter, null);

    ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(exception, request);

    assertEquals(400, response.getStatusCode().value());
    assertTrue(response.getBody().message().contains("status"));
  }

  @Test
  void unCuerpoNoLegibleResponde400() {
    ResponseEntity<ErrorResponse> response =
        handler.handleUnreadableBody(new HttpMessageNotReadableException("boom"), request);

    assertEquals(400, response.getStatusCode().value());
    assertEquals("Cuerpo de la peticion no valido", response.getBody().message());
  }

  @Test
  void unaRutaDesconocidaResponde404() {
    NoResourceFoundException ex =
        new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/");

    ResponseEntity<ErrorResponse> response = handler.handleNoResource(ex, request);

    assertEquals(404, response.getStatusCode().value());
    assertTrue(response.getBody().message().contains("/"));
  }

  @Test
  void unMetodoNoSoportadoResponde405() {
    ResponseEntity<ErrorResponse> response =
        handler.handleMethodNotSupported(
            new HttpRequestMethodNotSupportedException("PATCH"), request);

    assertEquals(405, response.getStatusCode().value());
    assertTrue(response.getBody().message().contains("PATCH"));
  }

  @Test
  void unTipoDeContenidoNoSoportadoResponde415() {
    ResponseEntity<ErrorResponse> response =
        handler.handleMediaTypeNotSupported(
            new HttpMediaTypeNotSupportedException("text/plain"), request);

    assertEquals(415, response.getStatusCode().value());
    assertTrue(response.getBody().message().contains("application/json"));
  }

  @Test
  void unErrorInesperadoResponde500ConMensajeGenerico() {
    ResponseEntity<ErrorResponse> response =
        handler.handleUnexpected(new IllegalStateException("detalle interno"), request);

    assertEquals(500, response.getStatusCode().value());
    assertEquals("Error interno del servidor", response.getBody().message());
  }
}
