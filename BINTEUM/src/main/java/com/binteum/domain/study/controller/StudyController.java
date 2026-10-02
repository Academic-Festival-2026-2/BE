package com.binteum.domain.study.controller;

import com.binteum.domain.study.dto.StudyCreateRequest;
import com.binteum.domain.study.dto.StudyDetailResponse;
import com.binteum.domain.study.dto.StudyResponse;
import com.binteum.domain.study.exception.code.StudySuccessCode;
import com.binteum.domain.study.service.StudyService;
import com.binteum.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
public class StudyController {

  private final StudyService studyService;

  @GetMapping("/{studyId}")
  public ResponseEntity<ApiResponse<StudyDetailResponse>> getStudy(@PathVariable Long studyId,
      @AuthenticationPrincipal Long userId) {
    StudyDetailResponse response = studyService.getStudy(studyId, userId);
    return ResponseEntity.ok(ApiResponse.of(StudySuccessCode._FOUND, response));

  }

  @PostMapping
  public ResponseEntity<ApiResponse<StudyResponse>> createStudy(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody StudyCreateRequest request) {
    StudyResponse response = studyService.createStudy(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.of(StudySuccessCode._CREATED, response));
  }
}


