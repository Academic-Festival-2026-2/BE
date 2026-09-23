package com.binteum.global.code;

import org.springframework.http.HttpStatus;

public enum SuccessCode {
  OK(HttpStatus.OK, "COMMON200", "요청이 성공적으로 처리되었습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  SuccessCode(HttpStatus httpStatus, String code, String message) {
    this.httpStatus = httpStatus;
    this.code = code;
    this.message = message;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}

