package com.pixel.domain.photo.controller;

import com.pixel.domain.photo.dto.MyPhotosResponse;
import com.pixel.domain.photo.dto.PhotoDetailResponse;
import com.pixel.domain.photo.dto.PhotoVisibilityRequest;
import com.pixel.domain.photo.exception.code.PhotoSuccessCode;
import com.pixel.domain.photo.service.PhotoManagementService;
import com.pixel.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PhotoManagementController {

  private final PhotoManagementService service;

  @GetMapping("/api/photos/{photoId}")
  public ResponseEntity<ApiResponse<PhotoDetailResponse>> detail(@PathVariable Long photoId,
      @AuthenticationPrincipal Long userId) {
    return ResponseEntity.ok(ApiResponse.of(PhotoSuccessCode._FOUND,
        service.detail(photoId, userId)));
  }

  @GetMapping("/api/users/me/photos")
  public ResponseEntity<ApiResponse<MyPhotosResponse>> myPhotos(
      @AuthenticationPrincipal Long userId, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(ApiResponse.of(PhotoSuccessCode._MY_PHOTOS,
        service.myPhotos(userId, page, size)));
  }

  @PatchMapping("/api/photos/{photoId}")
  public ResponseEntity<ApiResponse<Void>> changeVisibility(@PathVariable Long photoId,
      @AuthenticationPrincipal Long userId, @Valid @RequestBody PhotoVisibilityRequest request) {
    service.changeVisibility(photoId, userId, request.isPublic());
    return ResponseEntity.ok(ApiResponse.of(PhotoSuccessCode._UPDATED, null));
  }

  @DeleteMapping("/api/photos/{photoId}")
  public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long photoId,
      @AuthenticationPrincipal Long userId) {
    service.delete(photoId, userId);
    return ResponseEntity.ok(ApiResponse.of(PhotoSuccessCode._DELETED, null));
  }
}
