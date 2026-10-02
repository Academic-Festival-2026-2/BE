package com.binteum.domain.study.controller;

import com.binteum.domain.study.dto.StudyCreateRequest;
import com.binteum.domain.study.dto.StudyResponse;
import com.binteum.domain.study.enums.StudyCategory;
import com.binteum.domain.study.enums.StudyStatus;
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
  public ResponseEntity<StudyResponse> getStudy(@PathVariable Long studyId) {
    return ResponseEntity.ok(StudyResponse.builder().
        studyId(studyId)
        .title("알고리즘 스터디")
        .category(StudyCategory.IT)
        .maxParticipant(5)
        .status(StudyStatus.ACTIVE)
        .build());
  }

  @PostMapping
  public ResponseEntity<ApiResponse<StudyResponse>> createStudy(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody
      StudyCreateRequest request) {
    StudyResponse response = studyService.createStudy(userId, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.of(StudySuccessCode._CREATED, response));
  }
}


