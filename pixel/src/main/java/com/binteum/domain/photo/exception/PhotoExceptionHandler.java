package com.binteum.domain.photo.exception;

import com.binteum.domain.photo.controller.PhotoController;
import com.binteum.domain.photo.exception.code.PhotoErrorCode;
import com.binteum.global.apiPayload.ApiResponse;
import com.binteum.global.apiPayload.exception.GeneralException;
import com.binteum.global.apiPayload.exception.GlobalExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Order(-1)
@RestControllerAdvice(assignableTypes = PhotoController.class)
@RequiredArgsConstructor
public class PhotoExceptionHandler {
  private final GlobalExceptionHandler globalExceptionHandler;

  @ExceptionHandler({MissingServletRequestPartException.class, HttpMessageNotReadableException.class})
  public ResponseEntity<ApiResponse<Object>> handleInvalidRequest(Exception ex) {
    return globalExceptionHandler.handleGeneralException(
        new GeneralException(PhotoErrorCode.INVALID_MULTIPART));
  }

}
