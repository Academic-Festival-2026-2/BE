package com.pixel.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MyPageFlowTest {

  private static final String EMAIL = "mypage-flow@example.com";
  private static final String NICKNAME = "flowtester";
  private static final String INACTIVE_EMAIL = "inactive-flow@example.com";
  private static final String INACTIVE_NICKNAME = "inactiveuser";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void 비밀번호_변경_후_탈퇴하면_같은_이메일과_닉네임으로_다시_가입할_수_있다() throws Exception {
    signUp(EMAIL, NICKNAME, "password123").andExpect(status().isCreated());
    JsonNode tokens = login(EMAIL, "password123");
    String accessToken = tokens.get("accessToken").asText();
    String refreshToken = tokens.get("refreshToken").asText();

    mockMvc.perform(patch("/api/users/me/password")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"currentPassword\": \"password123\", \"newPassword\": \"newpass123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("USER200_8"));

    // 비밀번호를 바꾸면 이전 refresh token은 폐기된다
    mockMvc.perform(post("/api/auth/reissue")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("USER401_2"));

    accessToken = login(EMAIL, "newpass123").get("accessToken").asText();

    mockMvc.perform(delete("/api/users/me")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"password\": \"newpass123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("USER200_9"));

    // 탈퇴 후 남은 access token은 인증에 쓰이지 않는다
    mockMvc.perform(get("/api/users/me")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("COMMON401"));

    // 마이페이지 외 인증 API도 막힌다
    mockMvc.perform(multipart("/api/photos")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("COMMON401"));

    // 공개 API는 비로그인 사용자로 정상 응답한다
    mockMvc.perform(get("/health")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk());

    signUp(EMAIL, NICKNAME, "password123").andExpect(status().isCreated());
  }

  @Test
  void 비활성_회원의_토큰은_인증에_쓰이지_않고_로그인과_재발급도_막힌다() throws Exception {
    signUp(INACTIVE_EMAIL, INACTIVE_NICKNAME, "password123").andExpect(status().isCreated());
    JsonNode tokens = login(INACTIVE_EMAIL, "password123");
    String accessToken = tokens.get("accessToken").asText();
    String refreshToken = tokens.get("refreshToken").asText();

    // 상태를 바꾸는 API가 없어서 DB에서 직접 비활성으로 바꾼다
    jdbcTemplate.update("UPDATE users SET status = 'INACTIVE' WHERE email = ?", INACTIVE_EMAIL);

    mockMvc.perform(get("/api/users/me")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("COMMON401"));

    mockMvc.perform(post("/api/auth/reissue")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\": \"" + refreshToken + "\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("USER403_1"));

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\": \"%s\", \"password\": \"password123\"}"
                .formatted(INACTIVE_EMAIL)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("USER403_1"));
  }

  private ResultActions signUp(String email, String nickname, String password) throws Exception {
    String body = """
        {
          "email": "%s",
          "password": "%s",
          "nickname": "%s",
          "termsAgreed": true,
          "privacyAgreed": true
        }
        """.formatted(email, password, nickname);
    return mockMvc.perform(post("/api/auth/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body));
  }

  private JsonNode login(String email, String password) throws Exception {
    String response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(response).get("result");
  }
}
