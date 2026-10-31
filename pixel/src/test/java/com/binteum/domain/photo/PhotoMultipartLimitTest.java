package com.binteum.domain.photo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.binteum.domain.photo.controller.PhotoController;
import com.binteum.domain.photo.exception.PhotoExceptionHandler;
import com.binteum.domain.photo.service.PhotoService;
import com.binteum.global.apiPayload.exception.GlobalExceptionHandler;
import com.binteum.global.config.PhotoUploadConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = PhotoMultipartLimitTest.App.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PhotoMultipartLimitTest {
  @LocalServerPort int port;

  @Configuration
  @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,
      HibernateJpaAutoConfiguration.class, SecurityAutoConfiguration.class,
      UserDetailsServiceAutoConfiguration.class})
  @Import({PhotoController.class, PhotoUploadConfig.class, PhotoExceptionHandler.class,
      GlobalExceptionHandler.class})
  static class App {
    @Bean PhotoService photoService() { return mock(PhotoService.class); }
  }

  @ParameterizedTest
  @CsvSource({"0,401,PHOTO401_1", "1,413,PHOTO413_1"})
  void realMultipartParserEnforcesFileLimit(int extra, int status, String code) throws Exception {
    String boundary = "PixelBoundary";
    byte[] prefix = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"image\"; "
        + "filename=\"large.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n")
        .getBytes(StandardCharsets.US_ASCII);
    byte[] suffix = ("\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"data\"\r\n"
        + "Content-Type: application/json\r\n\r\n{}\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII);
    var body = new java.io.ByteArrayOutputStream();
    body.write(prefix);
    body.write(new byte[10 * 1024 * 1024 + extra]);
    body.write(suffix);
    var response = HttpClient.newHttpClient().send(
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/photos"))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),
        HttpResponse.BodyHandlers.ofString());
    assertEquals(status, response.statusCode());
    var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body());
    assertFalse(json.get("isSuccess").asBoolean());
    assertEquals(code, json.get("code").asText());
  }
}
