package com.binteum.global.code;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 내부 오류가 발생했습니다."),
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON400", "입력값이 올바르지 않습니다."),

  // User
  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER409_1", "이미 가입된 이메일입니다."),
  DUPLICATE_STUDENT_ID(HttpStatus.CONFLICT, "USER409_2", "이미 가입된 학번입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus httpStatus, String code, String message) {
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

