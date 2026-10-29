package com.binteum.domain.photo.controller;

import com.binteum.domain.photo.dto.PhotoUploadRequest;
import com.binteum.domain.photo.dto.PhotoUploadResponse;
import com.binteum.domain.photo.service.PhotoService;
import com.binteum.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/photos")
public class PhotoController {

  private final PhotoService photoService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<PhotoUploadResponse> upload(
      @RequestPart("file") MultipartFile file,
      @RequestPart("metadata") PhotoUploadRequest request,
      Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    PhotoUploadResponse response = photoService.upload(userId, file, request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiResponse<Object>> handleUploadError(ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode())
        .body(ApiResponse.onFailure("COMMON" + ex.getStatusCode().value(), ex.getReason(), null));
  }

  @ExceptionHandler({
    MissingServletRequestPartException.class,
    HttpMessageNotReadableException.class
  })
  public ResponseEntity<ApiResponse<Object>> handleInvalidRequest(Exception ex) {
    return ResponseEntity.badRequest()
        .body(ApiResponse.onFailure("COMMON400", "사진 파일과 올바른 메타데이터가 필요합니다.", null));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiResponse<Object>> handleUploadSize(MaxUploadSizeExceededException ex) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(ApiResponse.onFailure("COMMON413", "업로드 가능한 파일 크기를 초과했습니다.", null));
  }
}
