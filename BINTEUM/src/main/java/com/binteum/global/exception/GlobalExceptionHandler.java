package com.binteum.global.exception;

import com.binteum.global.apiPayload.ApiResponse;
import com.binteum.global.code.BaseErrorCode;
import com.binteum.global.code.ErrorCode;
import com.binteum.global.code.ErrorReasonDTO;
import java.util.LinkedHashMap;
import java.util.Map;
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
    ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
    return ResponseEntity
        .status(errorCode.getHttpStatus())
        .body(ApiResponse.onFailure(
            errorCode.getCode(),
            errorCode.getMessage(),
            ex.getMessage())); //디버깅용 메시지 (배포 시에는 null로 변경)
  }
}

