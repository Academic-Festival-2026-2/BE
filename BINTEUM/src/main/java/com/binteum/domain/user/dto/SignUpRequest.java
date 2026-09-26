package com.binteum.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignUpRequest {

  @NotBlank(message = "학번은 필수입니다.")
  @Size(max = 20, message = "학번은 20자 이하여야 합니다.")
  private String studentId;

  @NotBlank(message = "이름은 필수입니다.")
  @Size(max = 30, message = "이름은 30자 이하여야 합니다.")
  private String name;

  @NotBlank(message = "닉네임은 필수입니다.")
  @Size(max = 30, message = "닉네임은 30자 이하여야 합니다.")
  private String nickname;

  @NotBlank(message = "이메일은 필수입니다.")
  @Email(message = "이메일 형식이 올바르지 않습니다.")
  @Pattern(
      regexp = "^[A-Za-z0-9._%+-]+@catholic\\.ac\\.kr$",
      message = "가톨릭대학교 이메일(@catholic.ac.kr)만 사용할 수 있습니다.")
  @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
  private String email;

  @NotBlank(message = "비밀번호는 필수입니다.")
  @Pattern(
      regexp = "^[\\x21-\\x7E]{8,20}$",
      message = "비밀번호는 공백 없는 영문, 숫자, 특수문자 8~20자여야 합니다.")
  private String password;

  @Size(max = 50, message = "학과는 50자 이하여야 합니다.")
  private String department;
}