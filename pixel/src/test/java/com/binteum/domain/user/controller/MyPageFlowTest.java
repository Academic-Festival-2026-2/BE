package com.binteum.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MyPageFlowTest {

  private static final String EMAIL = "mypage-flow@example.com";
  private static final String NICKNAME = "flowtester";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void 비밀번호_변경_후_탈퇴하면_같은_이메일과_닉네임으로_다시_가입할_수_있다() throws Exception {
    signUp("password123").andExpect(status().isCreated());
    JsonNode tokens = login("password123");
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

    accessToken = login("newpass123").get("accessToken").asText();

    mockMvc.perform(delete("/api/users/me")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"password\": \"newpass123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("USER200_9"));

    // 탈퇴 후 남은 access token으로는 마이페이지를 이용할 수 없다
    mockMvc.perform(get("/api/users/me")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("USER403_1"));

    signUp("password123").andExpect(status().isCreated());
  }

  private ResultActions signUp(String password) throws Exception {
    String body = """
        {
          "email": "%s",
          "password": "%s",
          "nickname": "%s",
          "termsAgreed": true,
          "privacyAgreed": true
        }
        """.formatted(EMAIL, password, NICKNAME);
    return mockMvc.perform(post("/api/auth/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body));
  }

  private JsonNode login(String password) throws Exception {
    String response = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(EMAIL, password)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(response).get("result");
  }
}
