package com.customersupporthub.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    StringBuilder sb = new StringBuilder("Validation failed");
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      sb.append("; ").append(fe.getField()).append(": ").append(fe.getDefaultMessage());
    }
    return error(HttpStatus.BAD_REQUEST, sb.toString(), req);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest req) {
    return error(HttpStatus.UNAUTHORIZED, "Unauthorized", req);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
    return error(HttpStatus.FORBIDDEN, "Forbidden", req);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
    return error(HttpStatus.NOT_FOUND, ex.getMessage(), req);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest req) {
    return error(HttpStatus.CONFLICT, ex.getMessage(), req);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
      HttpServletRequest req) {
    return error(HttpStatus.CONFLICT, "Conflict", req);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
    return error(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
    log.error("Unhandled exception", ex);
    String msg = ex.getMessage();
    if (msg == null || msg.isBlank()) {
      msg = ex.getClass().getSimpleName();
    } else {
      msg = ex.getClass().getSimpleName() + ": " + msg;
    }
    return error(HttpStatus.INTERNAL_SERVER_ERROR, msg, req);
  }

  private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, HttpServletRequest req) {
    ApiErrorResponse body = new ApiErrorResponse(
        Instant.now(),
        status.value(),
        status.getReasonPhrase(),
        message,
        req.getRequestURI());
    return ResponseEntity.status(status).body(body);
  }
}
