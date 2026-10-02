package com.binteum.domain.study.exception.code;

import com.binteum.global.apiPayload.code.BaseErrorCode;
import com.binteum.global.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum StudyErrorCode implements BaseErrorCode {
  STUDY_TIME_PAST(HttpStatus.BAD_REQUEST, "STUDY400_1", "시작 시간은 현재 이후여야 합니다."),
  STUDY_TIME_ORDER(HttpStatus.BAD_REQUEST, "STUDY400_2", "종료 시간은 시작 시간보다 빠를 수 없습니다."),
  STUDY_TIME_DIFFERENT_DATE(HttpStatus.BAD_REQUEST, "STUDY400_3", "스터디는 당일에 끝나야 합니다.");

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
