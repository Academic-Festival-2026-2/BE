package com.binteum.domain.user.exception.code;

import com.binteum.global.apiPayload.code.BaseCode;
import com.binteum.global.apiPayload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserSuccessCode implements BaseCode {
  _FOUND(HttpStatus.OK, "USER200_1", "성공적으로 사용자를 조회했습니다."),
  _CREATED(HttpStatus.CREATED, "USER201_1", "회원가입에 성공하였습니다."),
  _LOGIN(HttpStatus.OK, "USER200_2", "로그인에 성공했습니다."),
  _TOKEN_REISSUED(HttpStatus.OK, "USER200_3", "토큰이 재발급되었습니다."),
  _LOGOUT(HttpStatus.OK, "USER200_4", "로그아웃되었습니다."),
  ;

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

  @Override
  public ReasonDTO getReason() {
    return ReasonDTO.builder()
        .message(message)
        .code(code)
        .isSuccess(true)
        .build();
  }

  @Override
  public ReasonDTO getReasonHttpStatus() {
    return ReasonDTO.builder()
        .message(message)
        .code(code)
        .isSuccess(true)
        .httpStatus(httpStatus)
        .build();
  }
}
