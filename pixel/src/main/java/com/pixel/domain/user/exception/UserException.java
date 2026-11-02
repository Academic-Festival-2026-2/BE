package com.pixel.domain.user.exception;

import com.pixel.global.apiPayload.code.BaseErrorCode;
import com.pixel.global.apiPayload.exception.GeneralException;

public class UserException extends GeneralException {
  public UserException(BaseErrorCode code) {
    super(code);
  }
}
