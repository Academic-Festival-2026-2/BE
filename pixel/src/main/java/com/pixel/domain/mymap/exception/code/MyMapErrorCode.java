package com.pixel.domain.mymap.exception.code;

import com.pixel.global.apiPayload.code.BaseErrorCode;
import com.pixel.global.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MyMapErrorCode implements BaseErrorCode {
  _SHARED_MAP_NOT_FOUND(HttpStatus.NOT_FOUND, "MYMAP404_1", "공유된 지도를 찾을 수 없습니다."),
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
