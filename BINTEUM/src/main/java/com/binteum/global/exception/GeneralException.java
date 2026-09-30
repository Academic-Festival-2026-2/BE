package com.binteum.global.exception;

import com.binteum.global.code.BaseErrorCode;
import com.binteum.global.code.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {
  private final BaseErrorCode code;
}
