package com.binteum.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WithdrawRequest {

  @NotBlank(message = "비밀번호는 필수입니다.")
  private String password;
}
