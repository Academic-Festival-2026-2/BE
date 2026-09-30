package com.binteum.global.code;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorReasonDTO {
  private String code;
  private String message;
  private int status;

  public ErrorReasonDTO(String code, String message, int status) {
    this.code = code;
    this.message = message;
    this.status = status;
  }

  // Getters and setters
}