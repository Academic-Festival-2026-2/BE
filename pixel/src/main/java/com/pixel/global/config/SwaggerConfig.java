package com.pixel.global.config;

import com.pixel.global.security.auth.PublicEndpoints;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  private static final String BEARER_AUTH = "bearerAuth";

  @Bean
  public OpenAPI pixelOpenAPI() {
    Info info = new Info()
        .title("PIXEL")
        .version("1.0")
        .description("PIXEL API 문서");

    SecurityScheme bearerAuth = new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT");

    return new OpenAPI()
        .addServersItem(new Server().url("/"))
        .components(new Components().addSecuritySchemes(BEARER_AUTH, bearerAuth))
        .info(info);
  }

  /**
   * 로그인이 필요한 API에만 자물쇠를 붙인다. 공개 여부는 SecurityConfig와 같은 PublicEndpoints로 판단한다.
   */
  @Bean
  public OpenApiCustomizer bearerAuthOnPrivateApis() {
    return openApi -> {
      if (openApi.getPaths() == null) {
        return;
      }
      openApi.getPaths().forEach((path, pathItem) ->
          pathItem.readOperationsMap().forEach((method, operation) -> {
            if (!PublicEndpoints.isPublic(method.name(), path)) {
              operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
            }
          }));
    };
  }
}
