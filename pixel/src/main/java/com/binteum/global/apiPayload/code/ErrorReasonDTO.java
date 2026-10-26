package com.binteum.global.apiPayload.code;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public class ErrorReasonDTO {
  private HttpStatus httpStatus;

  private final boolean isSuccess;
  private String code;
  private String message;

  public boolean getIsSuccess(){return isSuccess;}

  // Getters and setters
}