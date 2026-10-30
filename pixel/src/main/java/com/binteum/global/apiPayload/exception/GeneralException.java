package com.binteum.global.apiPayload.exception;

import com.binteum.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {
  private final BaseErrorCode code;
}
