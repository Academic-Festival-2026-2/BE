package com.binteum.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignUpRequest {

  @NotBlank
  @Size(max = 20)
  private String studentId;

  @NotBlank
  @Size(max = 30)
  private String name;

  @NotBlank
  @Size(max = 30)
  private String nickname;

  @NotBlank
  @Email
  @Size(max = 100)
  private String email;

  @NotBlank
  @Size(min = 8, max = 255)
  private String password;

  @Size(max = 50)
  private String department;
}


