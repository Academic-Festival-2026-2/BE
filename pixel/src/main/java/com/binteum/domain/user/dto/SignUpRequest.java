package com.binteum.domain.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignUpRequest {

  @NotBlank(message = "닉네임은 필수입니다.")
  @Size(min = 2, max = 16, message = "닉네임은 2~16자여야 합니다.")
  private String nickname;

  @NotBlank(message = "이메일은 필수입니다.")
  @Email(message = "이메일 형식이 올바르지 않습니다.")
  @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
  private String email;

  @NotBlank(message = "비밀번호는 필수입니다.")
  @Pattern(
      regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\x21-\\x7E]{8,20}$",
      message = "비밀번호는 영문과 숫자를 함께 포함한 8~20자여야 합니다.")
  private String password;

  @NotNull(message = "이용약관 동의는 필수입니다.")
  @AssertTrue(message = "이용약관 동의는 필수입니다.")
  private Boolean termsAgreed;

  @NotNull(message = "개인정보 수집·이용 동의는 필수입니다.")
  @AssertTrue(message = "개인정보 수집·이용 동의는 필수입니다.")
  private Boolean privacyAgreed;

  // 위치정보 이용 동의는 선택
  private Boolean locationAgreed;
}
