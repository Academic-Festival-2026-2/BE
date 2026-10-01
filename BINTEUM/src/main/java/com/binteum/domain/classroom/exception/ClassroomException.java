package com.binteum.domain.classroom.exception;

import com.binteum.global.apiPayload.code.BaseErrorCode;
import com.binteum.global.apiPayload.exception.GeneralException;

public class ClassroomException extends GeneralException {

  public ClassroomException(BaseErrorCode code) {
    super(code);
  }
}
