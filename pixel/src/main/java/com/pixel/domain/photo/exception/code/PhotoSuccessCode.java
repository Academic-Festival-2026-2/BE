package com.pixel.domain.photo.exception.code;

import com.pixel.global.apiPayload.code.BaseCode;
import com.pixel.global.apiPayload.code.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PhotoSuccessCode implements BaseCode {
  _FOUND(HttpStatus.OK, "PHOTO200_1", "사진을 조회했습니다."),
  _UPDATED(HttpStatus.OK, "PHOTO200_2", "사진 공개 여부를 변경했습니다."),
  _DELETED(HttpStatus.OK, "PHOTO200_3", "사진을 삭제했습니다."),
  _MY_PHOTOS(HttpStatus.OK, "PHOTO200_4", "내 사진 목록을 조회했습니다."),
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
