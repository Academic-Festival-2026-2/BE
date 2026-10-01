package com.binteum.domain.study.exception;

import com.binteum.global.code.BaseErrorCode;
import com.binteum.global.exception.GeneralException;

public class StudyException extends GeneralException {

  public StudyException(BaseErrorCode code) {
    super(code);
  }
}
