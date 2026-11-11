package com.pixel.domain.mymap.exception.code;

import com.pixel.global.apiPayload.code.BaseCode;
import com.pixel.global.apiPayload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MyMapSuccessCode implements BaseCode {
  _MY_MAP_FOUND(HttpStatus.OK, "MYMAP200_1", "나의 지도를 조회했습니다."),
  _MY_DONG_PHOTOS_FOUND(HttpStatus.OK, "MYMAP200_2", "동의 내 사진을 조회했습니다."),
  _MY_MAP_SHARED(HttpStatus.OK, "MYMAP200_3", "나의 지도 공유 링크를 만들었습니다."),
  _MY_MAP_SHARE_STOPPED(HttpStatus.OK, "MYMAP200_4", "나의 지도 공유를 중지했습니다."),
  _SHARED_MAP_FOUND(HttpStatus.OK, "MYMAP200_5", "공유된 지도를 조회했습니다."),
  _MY_MAP_SHARE_FOUND(HttpStatus.OK, "MYMAP200_6", "나의 지도 공유 상태를 조회했습니다."),
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
