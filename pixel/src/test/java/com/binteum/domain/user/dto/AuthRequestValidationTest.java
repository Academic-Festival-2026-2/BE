package com.binteum.domain.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AuthRequestValidationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Validator validator =
      Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void 닉네임_앞뒤_공백은_제거된다() throws Exception {
    SignUpRequest request = signUpRequest("  tester  ");

    assertThat(request.getNickname()).isEqualTo("tester");
    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void 공백을_제거한_닉네임이_2자_미만이면_실패한다() throws Exception {
    SignUpRequest request = signUpRequest("  a  ");

    assertThat(messages(validator.validate(request))).contains("닉네임은 2~16자여야 합니다.");
  }

  @Test
  void 공백만_있는_닉네임은_필수값_오류() throws Exception {
    SignUpRequest request = signUpRequest("   ");

    assertThat(messages(validator.validate(request))).contains("닉네임은 필수입니다.");
  }

  @Test
  void 로그인_요청_검증_메시지는_한글이다() throws Exception {
    LoginRequest request = objectMapper.readValue(
        "{\"email\": \"not-email\", \"password\": \"\"}", LoginRequest.class);

    assertThat(messages(validator.validate(request)))
        .containsExactlyInAnyOrder("이메일 형식이 올바르지 않습니다.", "비밀번호는 필수입니다.");
  }

  private SignUpRequest signUpRequest(String nickname) throws Exception {
    String json = """
        {
          "nickname": "%s",
          "email": "tester@example.com",
          "password": "password123",
          "termsAgreed": true,
          "privacyAgreed": true
        }
        """.formatted(nickname);
    return objectMapper.readValue(json, SignUpRequest.class);
  }

  private <T> Set<String> messages(Set<ConstraintViolation<T>> violations) {
    return violations.stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toSet());
  }
}
