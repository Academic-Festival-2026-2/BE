package com.binteum.domain.photo.controller;

import com.binteum.domain.photo.dto.PhotoUploadRequest;
import com.binteum.domain.photo.dto.PhotoUploadResponse;
import com.binteum.domain.photo.exception.code.PhotoErrorCode;
import com.binteum.domain.photo.exception.code.PhotoSuccessCode;
import com.binteum.domain.photo.service.PhotoService;
import com.binteum.global.apiPayload.ApiResponse;
import com.binteum.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/photos")
public class PhotoController {

  private final PhotoService photoService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<PhotoUploadResponse>> upload(
      @RequestPart("image") MultipartFile file,
      @RequestPart("data") PhotoUploadRequest request,
      Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
      throw new GeneralException(PhotoErrorCode.UNAUTHORIZED);
    }

    PhotoUploadResponse response = photoService.upload(userId, file, request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.of(PhotoSuccessCode._CREATED, response));
  }
}
