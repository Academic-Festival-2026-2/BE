package com.binteum.global.apiPayload.exception;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.binteum.domain.user.dto.SignUpRequest;
import com.binteum.domain.user.service.AuthService;
import java.sql.SQLIntegrityConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

  private static final String SIGNUP_BODY = """
      {
        "nickname": "tester",
        "email": "tester@example.com",
        "password": "password123",
        "termsAgreed": true,
        "privacyAgreed": true
      }
      """;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthService authService;

  @Test
  void 깨진_JSON이면_400() throws Exception {
    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\": "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON400"))
        .andExpect(jsonPath("$.result").doesNotExist());
  }

  @Test
  void 타입이_맞지_않으면_400과_필드_에러() throws Exception {
    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"termsAgreed\": \"abc\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON400"))
        .andExpect(jsonPath("$.result.termsAgreed").value("올바르지 않은 값입니다."));
  }

  @Test
  void 바디가_없으면_400() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON400"));
  }

  @Test
  void DB_중복_키_에러면_409() throws Exception {
    willThrow(integrityViolation(1062))
        .given(authService).signUp(any(SignUpRequest.class));

    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(SIGNUP_BODY))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("COMMON409"));
  }

  @Test
  void 중복_외_DB_제약_위반이면_500() throws Exception {
    // MySQL 3819: CHECK 제약 위반
    willThrow(integrityViolation(3819))
        .given(authService).signUp(any(SignUpRequest.class));

    mockMvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(SIGNUP_BODY))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("COMMON500"));
  }

  private DataIntegrityViolationException integrityViolation(int mysqlErrorCode) {
    return new DataIntegrityViolationException("constraint violation",
        new SQLIntegrityConstraintViolationException("constraint violation", "23000",
            mysqlErrorCode));
  }

  @Test
  void 없는_경로면_404() throws Exception {
    mockMvc.perform(post("/api/auth/unknown"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMMON404"));
  }

  @Test
  void 지원하지_않는_메서드면_405() throws Exception {
    mockMvc.perform(get("/api/auth/login"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", containsString("POST")))
        .andExpect(jsonPath("$.code").value("COMMON405"));
  }

  @Test
  void 지원하지_않는_Content_Type이면_415() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.TEXT_PLAIN)
            .content("email=a@test.com"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("COMMON415"));
  }
}
