package com.agentscustomerstickets.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    if (log.isDebugEnabled()) {
      StringBuilder sb = new StringBuilder("Validation failed");
      for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
        sb.append("; ").append(fe.getField()).append(": ").append(fe.getDefaultMessage());
      }
      log.debug("Validation failed: method={} path={} details={}", req.getMethod(), req.getRequestURI(), sb);
    }
    return error(HttpStatus.BAD_REQUEST, req);
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<ApiErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest req) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    log.info("Authentication failed: method={} path={} user={} authorities={} reason={}",
        req.getMethod(), req.getRequestURI(), principalName(auth), authorityNames(auth), ex.getMessage());
    return error(HttpStatus.UNAUTHORIZED, req);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiErrorResponse> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    log.info("Authorization denied: method={} path={} user={} authorities={} reason={}",
        req.getMethod(), req.getRequestURI(), principalName(auth), authorityNames(auth), ex.getMessage());
    return error(HttpStatus.FORBIDDEN, req);
  }

  @ExceptionHandler({ ResourceNotFoundException.class, NoResourceFoundException.class })
  ResponseEntity<ApiErrorResponse> handleNotFound(Exception ex, HttpServletRequest req) {
    return error(HttpStatus.NOT_FOUND, req);
  }

  @ExceptionHandler({ ConflictException.class, DataIntegrityViolationException.class })
  ResponseEntity<ApiErrorResponse> handleConflict(Exception ex, HttpServletRequest req) {
    return error(HttpStatus.CONFLICT, req);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
    return error(HttpStatus.BAD_REQUEST, req);
  }

  // Database availability handlers - matched by specificity, not order
  // Spring matches on the most specific exception type in the inheritance
  // hierarchy
  @ExceptionHandler({ CannotCreateTransactionException.class, DataAccessResourceFailureException.class })
  ResponseEntity<ApiErrorResponse> handleDatabaseUnavailable(Exception ex, HttpServletRequest req) {
    log.warn("Database unavailable: {}", ex.getClass().getSimpleName());
    return error(HttpStatus.SERVICE_UNAVAILABLE, req);
  }

  @ExceptionHandler(TransientDataAccessException.class)
  ResponseEntity<ApiErrorResponse> handleTransientDataAccessError(TransientDataAccessException ex,
      HttpServletRequest req) {
    log.warn("Transient database error (may be temporary): {}", ex.getMessage());
    return error(HttpStatus.SERVICE_UNAVAILABLE, req);
  }

  @ExceptionHandler(JpaSystemException.class)
  ResponseEntity<ApiErrorResponse> handleJpaSystemError(JpaSystemException ex, HttpServletRequest req) {
    log.warn("JPA system error: {}", ex.getMessage());
    // Check if the root cause is a database connectivity issue
    if (isCausedByDatabaseUnavailability(ex)) {
      return error(HttpStatus.SERVICE_UNAVAILABLE, req);
    }
    // If not a connection issue, treat as internal server error
    return error(HttpStatus.INTERNAL_SERVER_ERROR, req);
  }

  /**
   * Checks if an exception is caused by database unavailability by examining the
   * cause chain.
   * Looks for SQLException or connection-related exceptions in the stack.
   */
  private boolean isCausedByDatabaseUnavailability(Throwable ex) {
    Throwable cause = ex;
    while (cause != null) {
      // Check for JDBC connection errors
      if (cause instanceof SQLException) {
        return true;
      }
      // Check for Spring data access errors wrapping connection issues
      if (cause instanceof TransientDataAccessException ||
          cause instanceof DataAccessResourceFailureException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
    log.error("Unhandled exception", ex);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, req);
  }

  private ResponseEntity<ApiErrorResponse> error(HttpStatus status, HttpServletRequest req) {
    ApiErrorResponse body = new ApiErrorResponse(
        Instant.now(),
        status.value(),
        status.getReasonPhrase());
    return ResponseEntity.status(status).body(body);
  }

  private static String principalName(Authentication auth) {
    if (auth == null) {
      return "anonymous";
    }
    return auth.getName();
  }

  private static String authorityNames(Authentication auth) {
    if (auth == null || auth.getAuthorities() == null) {
      return "[]";
    }
    return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList().toString();
  }
}
