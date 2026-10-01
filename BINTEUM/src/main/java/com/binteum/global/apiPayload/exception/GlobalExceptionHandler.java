package com.binteum.global.apiPayload.exception;

import com.binteum.global.apiPayload.ApiResponse;
import com.binteum.global.apiPayload.code.BaseErrorCode;
import com.binteum.global.apiPayload.code.ErrorCode;
import com.binteum.global.apiPayload.code.ErrorReasonDTO;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
    ErrorCode errorCode = ErrorCode.INVALID_INPUT;
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
    }
    return ResponseEntity
        .status(errorCode.getHttpStatus())
        .body(ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage(), errors));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
    // 에러 추적용 고유 ID 생성
    String errorTraceId = UUID.randomUUID().toString();
    // 서버 로그(콘솔 또는 파일)에는 에러의 원문 스택 트레이스와 Trace ID를 모두 기록
    log.error("[TraceID: {}] 500 Internal Server Error: {}", errorTraceId, ex.getMessage(), ex);

    ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    return ResponseEntity
        .status(errorCode.getHttpStatus())
        .body(ApiResponse.onFailure(
            errorCode.getCode(),
            errorCode.getMessage(),
            "Error Trace ID: " + errorTraceId
        ));
  }
}

