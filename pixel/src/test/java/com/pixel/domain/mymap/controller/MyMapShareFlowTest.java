package com.pixel.domain.mymap.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class MyMapShareFlowTest {

  private static final String PASSWORD = "password123";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void sharingTwiceKeepsSameKey() throws Exception {
    String email = "share-twice@example.com";
    String token = signUpAndLogin(email, "sharetwice");

    String first = share(token);
    String second = share(token);

    assertThat(second).isEqualTo(first);
    assertThat(first).hasSize(32);

    // @Transactional이 빠지면 응답에는 키가 오지만 DB에는 저장되지 않는다.
    String saved = jdbcTemplate.queryForObject(
        "SELECT share_key FROM users WHERE email = ?",
        String.class, email);
    assertThat(saved).isEqualTo(first);
  }

  @Test
  void stoppedLinkReturns404WithoutLogin() throws Exception {
    String token = signUpAndLogin("share-stop@example.com", "sharestop");
    String key = share(token);

    mockMvc.perform(delete("/api/users/me/map/share")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("MYMAP200_4"));

    mockMvc.perform(get("/api/shared-maps/" + key))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("MYMAP404_1"));
  }

  @Test
  void inactiveUserLinkReturns404() throws Exception {
    String email = "share-inactive@example.com";
    String key = share(signUpAndLogin(email, "shareinactive"));
    jdbcTemplate.update("UPDATE users SET status = 'INACTIVE' WHERE email = ?", email);

    mockMvc.perform(get("/api/shared-maps/" + key))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("MYMAP404_1"));
  }

  @Test
  void myShareShowsKeyOnlyWhileSharing() throws Exception {
    String token = signUpAndLogin("share-status@example.com", "sharestatus");

    myShare(token).andExpect(jsonPath("$.result.shareKey").value(nullValue()));

    String key = share(token);

    myShare(token).andExpect(jsonPath("$.result.shareKey").value(key));

    mockMvc.perform(delete("/api/users/me/map/share")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    myShare(token).andExpect(jsonPath("$.result.shareKey").value(nullValue()));
  }

  private ResultActions myShare(String token) throws Exception {
    return mockMvc.perform(get("/api/users/me/map/share")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("MYMAP200_6"));
  }

  private String signUpAndLogin(String email, String nickname) throws Exception {
    signUp(email, nickname, PASSWORD).andExpect(status().isCreated());
    return login(email, PASSWORD).get("accessToken").asText();
  }

  private String share(String token) throws Exception {
    String response = mockMvc.perform(post("/api/users/me/map/share")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    return objectMapper.readTree(response).get("result").get("shareKey").asText();
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
