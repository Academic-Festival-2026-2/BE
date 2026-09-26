package com.binteum.global.exception;

import com.binteum.global.code.ErrorCode;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {
  private final ErrorCode errorCode;

  public GeneralException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }
}
