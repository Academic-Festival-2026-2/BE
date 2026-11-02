package com.binteum.domain.photo.exception.code;

import com.binteum.global.apiPayload.code.BaseCode;
import com.binteum.global.apiPayload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PhotoSuccessCode implements BaseCode {
  _CREATED(HttpStatus.CREATED, "PHOTO201_1", "사진 업로드에 성공했습니다.");

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
