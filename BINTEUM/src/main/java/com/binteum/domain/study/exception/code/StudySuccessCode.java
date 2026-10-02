package com.binteum.domain.study.exception.code;

import com.binteum.global.apiPayload.code.BaseCode;
import com.binteum.global.apiPayload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StudySuccessCode implements BaseCode {
  _CREATED(HttpStatus.CREATED, "STUDY201_1", "스터디가 생성되었습니다."),
  _FOUND(HttpStatus.OK, "STUDY200_1", "스터디를 조회했습니다.");
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
