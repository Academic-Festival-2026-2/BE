package com.binteum.domain.user.exception;

import com.binteum.global.code.BaseErrorCode;
import com.binteum.global.exception.GeneralException;

public class UserException extends GeneralException {
  public UserException(BaseErrorCode code) {
    super(code);
  }
}
