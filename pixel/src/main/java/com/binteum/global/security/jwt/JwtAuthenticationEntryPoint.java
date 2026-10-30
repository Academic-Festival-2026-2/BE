package com.binteum.global.security.jwt;

import com.binteum.global.apiPayload.ApiResponse;
import com.binteum.global.apiPayload.status.ErrorStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException authException) throws IOException {
    ErrorStatus errorStatus = ErrorStatus._UNAUTHORIZED;

    response.setStatus(errorStatus.getHttpStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    ApiResponse<Object> body =
        ApiResponse.onFailure(errorStatus.getCode(), errorStatus.getMessage(), null);
    objectMapper.writeValue(response.getWriter(), body);
  }
}