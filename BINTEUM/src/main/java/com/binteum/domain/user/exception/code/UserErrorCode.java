package com.binteum.domain.user.exception.code;

import com.binteum.global.code.BaseErrorCode;
import com.binteum.global.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
  _NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER400", "사용자가 존재하지 않습니다"),
  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER409_1", "이미 가입된 이메일입니다."),
  DUPLICATE_STUDENT_ID(HttpStatus.CONFLICT, "USER409_2", "이미 가입된 학번입니다."),
  ;

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public ErrorReasonDTO getReason() {
    return ErrorReasonDTO.builder()
        .message(message)
        .code(code)
        .isSuccess(false)
        .build();
  }

  @Override
  public ErrorReasonDTO getReasonHttpStatus() {
    return ErrorReasonDTO.builder()
        .message(message)
        .code(code)
        .isSuccess(false)
        .httpStatus(httpStatus)
        .build();
  }
}
