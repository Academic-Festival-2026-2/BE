package com.binteum.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChangePasswordRequest {

  @NotBlank(message = "현재 비밀번호는 필수입니다.")
  private String currentPassword;

  @NotBlank(message = "새 비밀번호는 필수입니다.")
  @Pattern(
      regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,20}$",
      message = "비밀번호는 영문과 숫자를 함께 포함한 8~20자여야 합니다.")
  private String newPassword;
}
