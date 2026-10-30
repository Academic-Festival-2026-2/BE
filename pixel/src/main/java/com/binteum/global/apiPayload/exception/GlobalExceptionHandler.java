package com.binteum.global.apiPayload.exception;

import com.binteum.global.apiPayload.ApiResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.binteum.global.apiPayload.code.BaseErrorCode;
import com.binteum.global.apiPayload.code.ErrorReasonDTO;
import com.binteum.global.apiPayload.status.ErrorStatus;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(GeneralException.class)
  public ResponseEntity<ApiResponse<Object>> handleGeneralException(GeneralException ex) {
    BaseErrorCode code = ex.getCode();
    ErrorReasonDTO reason = code.getReasonHttpStatus();

    log.warn("[GeneralException] Code: {}, Message: {}", reason.getCode(), reason.getMessage());

    return ResponseEntity
        .status(reason.getHttpStatus())
        .body(ApiResponse.onFailure(reason.getCode(), reason.getMessage(), null));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Object>> handleValidationException(
      MethodArgumentNotValidException ex) {
    ErrorStatus errorStatus = ErrorStatus._BAD_REQUEST;
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
    }
    return ResponseEntity
        .status(errorStatus.getHttpStatus())
        .body(ApiResponse.onFailure(errorStatus.getCode(), errorStatus.getMessage(), errors));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiResponse<Object>> handleMissingParameter(
      MissingServletRequestParameterException ex) {
    ErrorStatus errorStatus = ErrorStatus._BAD_REQUEST;
    Map<String, String> errors = Map.of(ex.getParameterName(), "필수 값입니다.");
    return ResponseEntity.status(errorStatus.getHttpStatus())
        .body(ApiResponse.onFailure(errorStatus.getCode(), errorStatus.getMessage(), errors));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex) {
    ErrorStatus errorStatus = ErrorStatus._BAD_REQUEST;
    Map<String, String> errors = Map.of(ex.getName(), typeMismatchMessage(ex));
    return ResponseEntity.status(errorStatus.getHttpStatus())
        .body(ApiResponse.onFailure(errorStatus.getCode(), errorStatus.getMessage(), errors));
  }

  private String typeMismatchMessage(MethodArgumentTypeMismatchException ex) {
    Class<?> type = ex.getRequiredType();
    if (type != null && type.isEnum()) {
      String values = Arrays.stream(type.getEnumConstants())
          .map(String::valueOf)
          .collect(Collectors.joining(", "));
      return values + " 중 하나여야 합니다.";
    }
    return "올바르지 않은 값입니다.";
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Object>> handleNotReadable(
      HttpMessageNotReadableException ex) {
    ErrorStatus errorStatus = ErrorStatus._BAD_REQUEST;
    return ResponseEntity.status(errorStatus.getHttpStatus())
        .body(ApiResponse.onFailure(
            errorStatus.getCode(), "요청 본문 형식이 올바르지 않습니다.", notReadableErrors(ex)));
  }

  // 타입이 맞지 않는 필드가 있으면 fieldErrors로, 깨진 JSON·빈 바디면 null
  private Map<String, String> notReadableErrors(HttpMessageNotReadableException ex) {
    if (ex.getCause() instanceof MismatchedInputException cause) {
      String field = cause.getPath().stream()
          .map(JsonMappingException.Reference::getFieldName)
          .filter(Objects::nonNull)
          .collect(Collectors.joining("."));
      if (!field.isEmpty()) {
        return Map.of(field, "올바르지 않은 값입니다.");
      }
    }
    return null;
  }

  // 동시 요청이 사전 중복 검사를 함께 통과해 DB UNIQUE 제약에 걸린 경우
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(
      DataIntegrityViolationException ex) {
    log.warn("[DataIntegrityViolation] {}", ex.getMostSpecificCause().getMessage());

    ErrorStatus errorStatus = ErrorStatus._CONFLICT;
    return ResponseEntity.status(errorStatus.getHttpStatus())
        .body(ApiResponse.onFailure(errorStatus.getCode(), errorStatus.getMessage(), null));
  }

  // Spring MVC 기본 예외가 아래 Exception 핸들러로 떨어져 500이 되지 않도록 상태 코드를 맞춰 응답
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Object>> handleNoResource(NoResourceFoundException ex) {
    return failure(ErrorStatus._NOT_FOUND);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex) {
    return failure(ErrorStatus._METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiResponse<Object>> handleMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex) {
    return failure(ErrorStatus._UNSUPPORTED_MEDIA_TYPE);
  }

  private ResponseEntity<ApiResponse<Object>> failure(ErrorStatus errorStatus) {
    return ResponseEntity.status(errorStatus.getHttpStatus())
        .body(ApiResponse.onFailure(errorStatus.getCode(), errorStatus.getMessage(), null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
    // 에러 추적용 고유 ID 생성
    String errorTraceId = UUID.randomUUID().toString();
    // 서버 로그(콘솔 또는 파일)에는 에러의 원문 스택 트레이스와 Trace ID를 모두 기록
    log.error("[TraceID: {}] 500 Internal Server Error: {}", errorTraceId, ex.getMessage(), ex);

    ErrorStatus errorStatus = ErrorStatus._INTERNAL_SERVER_ERROR;
    return ResponseEntity
        .status(errorStatus.getHttpStatus())
        .body(ApiResponse.onFailure(
            errorStatus.getCode(),
            errorStatus.getMessage(),
            "Error Trace ID: " + errorTraceId
        ));
  }
}

