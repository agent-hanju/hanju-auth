package me.hanju.auth.authenticator.config;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import me.hanju.auth.authenticator.dto.ErrorResponse;
import me.hanju.auth.authenticator.exception.AccountAlreadyExistsException;
import me.hanju.auth.authenticator.exception.AuthenticationException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 전역 예외 처리 Advice
 * <p>
 * Controller에서 발생하는 예외를 catch하여 적절한 HTTP 응답으로 변환합니다.
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalControllerAdvice {

  /**
   * 인증 실패 예외 처리
   *
   * @param ex      인증 예외
   * @param request HTTP 요청
   * @return 401 Unauthorized 응답
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      AuthenticationException ex,
      HttpServletRequest request) {

    log.warn("Authentication failed: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.UNAUTHORIZED.value())
        .message(ex.getMessage())
        .build();

    return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(errorResponse);
  }

  /**
   * 권한 부족 예외 처리 (Spring Security)
   *
   * @param ex      권한 예외
   * @param request HTTP 요청
   * @return 403 Forbidden 응답
   */
  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(
      AuthorizationDeniedException ex,
      HttpServletRequest request) {

    log.warn("Authorization failed: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.FORBIDDEN.value())
        .message("Access denied")
        .build();

    return ResponseEntity
        .status(HttpStatus.FORBIDDEN)
        .body(errorResponse);
  }

  /**
   * 사용자 중복 예외 처리
   *
   * @param ex      중복 예외
   * @param request HTTP 요청
   * @return 409 Conflict 응답
   */
  @ExceptionHandler(AccountAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(
      AccountAlreadyExistsException ex,
      HttpServletRequest request) {

    log.warn("User already exists: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.CONFLICT.value())
        .message(ex.getMessage())
        .build();

    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(errorResponse);
  }

  /**
   * 엔티티 없음 예외 처리 (NoSuchElementException)
   *
   * @param ex      NoSuchElementException
   * @param request HTTP 요청
   * @return 404 Not Found 응답
   */
  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ErrorResponse> handleNoSuchElementException(
      NoSuchElementException ex,
      HttpServletRequest request) {

    log.warn("Resource not found: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.NOT_FOUND.value())
        .message(ex.getMessage() != null ? ex.getMessage() : "Resource not found")
        .build();

    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(errorResponse);
  }

  /**
   * Validation 실패 처리 (@Valid, @NotBlank 등)
   *
   * @param ex      MethodArgumentNotValidException
   * @param request HTTP 요청
   * @return 400 Bad Request 응답
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex,
      HttpServletRequest request) {

    // 첫 번째 validation error 메시지 사용
    String message = "Invalid request";
    if (ex.getBindingResult().hasErrors()) {
      FieldError fieldError = ex.getBindingResult().getFieldError();
      if (fieldError != null) {
        message = fieldError.getDefaultMessage();
      }
    }

    log.warn("Validation failed: {} - Path: {}", message, request.getRequestURI());

    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .message(message)
        .build();

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(errorResponse);
  }

  /**
   * 잘못된 요청 본문 처리 (Invalid JSON)
   *
   * @param ex      HttpMessageNotReadableException
   * @param request HTTP 요청
   * @return 400 Bad Request 응답
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex,
      HttpServletRequest request) {

    log.warn("Invalid request body: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .message("Invalid request body")
        .build();

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(errorResponse);
  }

  /**
   * 잘못된 인자 예외 처리 (IllegalArgumentException)
   * <p>
   * Enum 변환 실패, 잘못된 파라미터 값 등 클라이언트 입력 오류를 처리합니다.
   * 예: AuthRole.fromString("INVALID_ROLE")
   * </p>
   *
   * @param ex      IllegalArgumentException
   * @param request HTTP 요청
   * @return 400 Bad Request 응답
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
      IllegalArgumentException ex,
      HttpServletRequest request) {

    log.warn("Invalid argument: {} - Path: {}", ex.getMessage(), request.getRequestURI());

    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .message(ex.getMessage())
        .build();

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(errorResponse);
  }

  /**
   * 일반 예외 처리 (catch-all)
   *
   * @param ex      일반 예외
   * @param request HTTP 요청
   * @return 500 Internal Server Error 응답
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(
      Exception ex,
      HttpServletRequest request) {

    log.error("Unexpected error occurred: {} - Path: {}", ex.getMessage(), request.getRequestURI(), ex);

    ErrorResponse errorResponse = ErrorResponse.builder()
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .message("An unexpected error occurred")
        .build();

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(errorResponse);
  }
}
