package com.example.todo.web;

import com.example.todo.exception.InvalidStatusTransitionException;
import com.example.todo.exception.TaskNotFoundException;
import com.example.todo.exception.TaskStateException;
import com.example.todo.exception.TaskValidationException;
import com.example.todo.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(TaskNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(
      TaskNotFoundException ex, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
  }

  @ExceptionHandler(TaskValidationException.class)
  public ResponseEntity<ErrorResponse> handleBusinessValidation(
      TaskValidationException ex, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
  }

  @ExceptionHandler(InvalidStatusTransitionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidTransition(
      InvalidStatusTransitionException ex, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
  }

  @ExceptionHandler(TaskStateException.class)
  public ResponseEntity<ErrorResponse> handleConflict(
      TaskStateException ex, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResource(
      NoResourceFoundException ex, HttpServletRequest request) {
    return build(
        HttpStatus.NOT_FOUND,
        "No existe el recurso solicitado: " + ex.getResourcePath(),
        request,
        null);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoHandler(
      NoHandlerFoundException ex, HttpServletRequest request) {
    return build(
        HttpStatus.NOT_FOUND,
        "No hay handler para " + ex.getHttpMethod() + " " + ex.getRequestURL(),
        request,
        null);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    return build(
        HttpStatus.METHOD_NOT_ALLOWED,
        "Metodo " + ex.getMethod() + " no soportado en este recurso",
        request,
        null);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
    return build(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "Tipo de contenido no soportado, use application/json",
        request,
        null);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleBeanValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
    return build(HttpStatus.BAD_REQUEST, "Validacion fallida", request, fieldErrors);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    String message = "Valor invalido para el parametro '" + ex.getName() + "'";
    return build(HttpStatus.BAD_REQUEST, message, request, null);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableBody(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, "Cuerpo de la peticion no valido", request, null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request, null);
  }

  private ResponseEntity<ErrorResponse> build(
      HttpStatus status,
      String message,
      HttpServletRequest request,
      Map<String, String> fieldErrors) {
    ErrorResponse body =
        new ErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            message,
            request.getRequestURI(),
            fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors));
    return ResponseEntity.status(status).body(body);
  }
}
