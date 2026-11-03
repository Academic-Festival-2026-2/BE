package com.pixel.global.apiPayload.exception;

import com.pixel.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {
  private final BaseErrorCode code;
}
