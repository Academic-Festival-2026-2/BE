package com.binteum.domain.map.exception.code;

import com.binteum.global.apiPayload.code.BaseCode;
import com.binteum.global.apiPayload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MapSuccessCode implements BaseCode {
  _BOUNDARIES_FOUND(HttpStatus.OK, "MAP200_2","부천 동 경계 조회 완료"),;

  private HttpStatus httpStatus;
  private String code;
  private String message;

  @Override
  public ReasonDTO getReason() {
    return ReasonDTO.builder()
        .message(message)
        .code(code)
        .isSuccess(true)
        .build();
  }

  @Override
  public ReasonDTO getReasonHttpStatus(){
    return ReasonDTO.builder()
        .message(message)
        .code(code)
        .isSuccess(true)
        .httpStatus(httpStatus)
        .build();
  }

}
