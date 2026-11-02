package com.binteum.domain.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MyPageRequestValidationTest {

  private static final String RESERVED_NICKNAME = "사용할 수 없는 닉네임입니다.";

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Validator validator =
      Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void 수정_닉네임_앞뒤_공백은_제거된다() throws Exception {
    UpdateMyInfoRequest request = updateRequest("{\"nickname\": \"  tester  \"}");

    assertThat(request.getNickname()).isEqualTo("tester");
    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void 탈퇴회원으로_시작하는_닉네임으로_수정할_수_없다() throws Exception {
    UpdateMyInfoRequest request = updateRequest("{\"nickname\": \"탈퇴회원99\"}");

    assertThat(messages(validator.validate(request))).contains(RESERVED_NICKNAME);
  }

  @Test
  void 앞에_공백을_넣어도_탈퇴회원_닉네임은_막힌다() throws Exception {
    UpdateMyInfoRequest request = updateRequest("{\"nickname\": \"  탈퇴회원99\"}");

    assertThat(messages(validator.validate(request))).contains(RESERVED_NICKNAME);
  }

  @Test
  void 탈퇴회원이_가운데_들어간_닉네임은_허용한다() throws Exception {
    UpdateMyInfoRequest request = updateRequest("{\"nickname\": \"나는탈퇴회원\"}");

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void 닉네임을_보내지_않으면_닉네임은_검증하지_않는다() throws Exception {
    UpdateMyInfoRequest request = updateRequest("{\"locationAgreed\": true}");

    assertThat(request.getNickname()).isNull();
    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void 탈퇴회원으로_시작하는_닉네임으로_가입할_수_없다() throws Exception {
    SignUpRequest request = signUpRequest("deleted@example.com", "탈퇴회원99");

    assertThat(messages(validator.validate(request))).contains(RESERVED_NICKNAME);
  }

  @Test
  void 탈퇴_회원의_익명_이메일로는_가입할_수_없다() throws Exception {
    SignUpRequest request = signUpRequest("deleted:1", "tester");

    assertThat(messages(validator.validate(request))).contains("이메일 형식이 올바르지 않습니다.");
  }

  @Test
  void 새_비밀번호가_규칙에_맞지_않으면_실패한다() throws Exception {
    ChangePasswordRequest request = objectMapper.readValue(
        "{\"currentPassword\": \"password123\", \"newPassword\": \"abc\"}",
        ChangePasswordRequest.class);

    assertThat(messages(validator.validate(request)))
        .contains("비밀번호는 영문과 숫자를 함께 포함한 8~20자여야 합니다.");
  }

  private UpdateMyInfoRequest updateRequest(String json) throws Exception {
    return objectMapper.readValue(json, UpdateMyInfoRequest.class);
  }

  private SignUpRequest signUpRequest(String email, String nickname) throws Exception {
    String json = """
        {
          "nickname": "%s",
          "email": "%s",
          "password": "password123",
          "termsAgreed": true,
          "privacyAgreed": true
        }
        """.formatted(nickname, email);
    return objectMapper.readValue(json, SignUpRequest.class);
  }

  private <T> Set<String> messages(Set<ConstraintViolation<T>> violations) {
    return violations.stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toSet());
  }
}
