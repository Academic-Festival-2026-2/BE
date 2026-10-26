package com.binteum.domain.user.exception.code;

import com.binteum.global.apiPayload.code.BaseErrorCode;
import com.binteum.global.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
  _NOT_FOUND(HttpStatus.NOT_FOUND, "USER404_1", "사용자가 존재하지 않습니다"),
  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER409_1", "이미 가입된 이메일입니다."),
  DUPLICATE_STUDENT_ID(HttpStatus.CONFLICT, "USER409_2", "이미 가입된 학번입니다."),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "USER401_1", "이메일 또는 비밀번호가 올바르지 않습니다."),
  INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "USER401_2", "유효하지 않은 리프레시 토큰입니다."),
  INACTIVE_USER(HttpStatus.FORBIDDEN, "USER403_1", "이용할 수 없는 계정입니다."),
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
