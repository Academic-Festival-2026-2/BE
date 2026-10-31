package com.binteum.domain.photo.exception.code;

import com.binteum.global.apiPayload.code.BaseErrorCode;
import com.binteum.global.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PhotoErrorCode implements BaseErrorCode {
  FILE_REQUIRED(HttpStatus.BAD_REQUEST, "PHOTO400_1", "사진 파일이 필요합니다."),
  FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "PHOTO400_2", "사진은 최대 10MB까지 가능합니다."),
  METADATA_REQUIRED(HttpStatus.BAD_REQUEST, "PHOTO400_3", "사진 메타데이터가 필요합니다."),
  FILE_UNREADABLE(HttpStatus.BAD_REQUEST, "PHOTO400_4", "사진을 읽지 못했습니다."),
  TAKEN_AT_REQUIRED(HttpStatus.BAD_REQUEST, "PHOTO400_5", "촬영 날짜와 시간을 입력해야 합니다."),
  SHOT_DATE_MISMATCH(HttpStatus.BAD_REQUEST, "PHOTO400_6", "촬영 날짜가 촬영 일시와 다릅니다."),
  GPS_PAIR_REQUIRED(HttpStatus.BAD_REQUEST, "PHOTO400_7", "위도와 경도는 함께 입력해야 합니다."),
  GPS_OUTSIDE_AREA(HttpStatus.BAD_REQUEST, "PHOTO400_8", "부천시 범위 밖의 GPS입니다."),
  DONG_GPS_MISMATCH(HttpStatus.BAD_REQUEST, "PHOTO400_9", "선택한 행정동과 GPS 위치가 다릅니다."),
  DONG_REQUIRED(HttpStatus.BAD_REQUEST, "PHOTO400_10", "행정동을 선택해야 합니다."),
  DONG_NOT_FOUND(HttpStatus.BAD_REQUEST, "PHOTO400_11", "존재하지 않는 행정동입니다."),
  SEASON_NOT_FOUND(HttpStatus.BAD_REQUEST, "PHOTO400_12", "해당 날짜에 등록된 계절이 없습니다."),
  INVALID_IMAGE(HttpStatus.BAD_REQUEST, "PHOTO400_13", "이미지를 처리할 수 없습니다."),
  INVALID_PALETTE_SIZE(HttpStatus.BAD_REQUEST, "PHOTO400_14", "palette는 색상 5개가 필요합니다."),
  INVALID_PALETTE_COLOR(HttpStatus.BAD_REQUEST, "PHOTO400_15", "색상은 #RRGGBB 대문자 형식이어야 합니다."),
  INVALID_BINS_SIZE(HttpStatus.BAD_REQUEST, "PHOTO400_16", "bins는 40개 숫자가 필요합니다."),
  INVALID_BINS_VALUE(HttpStatus.BAD_REQUEST, "PHOTO400_17", "bins 값은 0~1이어야 합니다."),
  INVALID_BINS_SUM(HttpStatus.BAD_REQUEST, "PHOTO400_18", "bins의 합은 1이어야 합니다."),
  INVALID_COORDINATES(HttpStatus.BAD_REQUEST, "PHOTO400_19", "올바르지 않은 GPS 좌표입니다."),
  UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "PHOTO400_20", "JPEG 또는 PNG 파일만 지원합니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "PHOTO401_1", "로그인 사용자를 찾을 수 없습니다."),
  DUPLICATE_PHOTO(HttpStatus.CONFLICT, "PHOTO409_1", "이미 등록된 사진입니다."),
  INVALID_MULTIPART(HttpStatus.BAD_REQUEST, "PHOTO400_21", "사진 파일과 올바른 메타데이터가 필요합니다."),
  PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "PHOTO413_1", "업로드 가능한 파일 크기를 초과했습니다.");

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
