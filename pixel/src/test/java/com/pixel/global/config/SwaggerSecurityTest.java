package com.pixel.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerSecurityTest {

  private static final String SECURITY_UNAUTHORIZED_CODE = "COMMON401";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void registersBearerSchemeWithoutGlobalRequirement() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
        .andExpect(jsonPath("$.security").doesNotExist());
  }

  // 문서의 모든 API를 토큰 없이 호출해, 인증 필터에서 막히는 API에만 자물쇠가 붙었는지 확인한다.
  @Test
  void locksOnlyApisThatRequireLogin() throws Exception {
    String docs = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse()
        .getContentAsString();
    JsonNode paths = objectMapper.readTree(docs).get("paths");

    List<String> mismatches = new ArrayList<>();
    int locked = 0;
    int open = 0;
    for (Map.Entry<String, JsonNode> path : paths.properties()) {
      for (Map.Entry<String, JsonNode> operation : path.getValue().properties()) {
        String method = operation.getKey().toUpperCase();
        boolean hasLock = operation.getValue().has("security");
        boolean requiresLogin = blockedWithoutToken(method, path.getKey());
        if (hasLock != requiresLogin) {
          mismatches.add(method + " " + path.getKey() + " lock=" + hasLock
              + " requiresLogin=" + requiresLogin);
        }
        if (hasLock) {
          locked++;
        } else {
          open++;
        }
      }
    }

    assertThat(mismatches).isEmpty();
    assertThat(locked).isPositive();
    assertThat(open).isPositive();
  }

  private boolean blockedWithoutToken(String method, String pathTemplate) throws Exception {
    String url = pathTemplate.replaceAll("\\{[^/]+}", "1");
    MockHttpServletResponse response =
        mockMvc.perform(request(HttpMethod.valueOf(method), url)).andReturn().getResponse();
    if (response.getStatus() != 401) {
      return false;
    }
    JsonNode body = objectMapper.readTree(response.getContentAsString());
    return SECURITY_UNAUTHORIZED_CODE.equals(body.path("code").asText());
  }
}
