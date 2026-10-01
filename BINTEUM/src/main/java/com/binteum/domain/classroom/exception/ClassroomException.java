package com.binteum.domain.classroom.exception;

import com.binteum.global.code.BaseErrorCode;
import com.binteum.global.exception.GeneralException;

public class ClassroomException extends GeneralException {

  public ClassroomException(BaseErrorCode code) {
    super(code);
  }
}
