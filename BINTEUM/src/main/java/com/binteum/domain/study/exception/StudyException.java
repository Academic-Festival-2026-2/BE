package com.binteum.domain.study.exception;

import com.binteum.global.apiPayload.code.BaseErrorCode;
import com.binteum.global.apiPayload.exception.GeneralException;

public class StudyException extends GeneralException {

  public StudyException(BaseErrorCode code) {
    super(code);
  }
}
