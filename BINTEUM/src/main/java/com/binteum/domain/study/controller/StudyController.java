package com.binteum.domain.study.controller;

import com.binteum.domain.study.dto.StudyCreateRequest;
import com.binteum.domain.study.dto.StudyResponse;
import com.binteum.domain.study.enums.StudyCategory;
import com.binteum.domain.study.enums.StudyStatus;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/studies")
public class StudyController {

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
  public ResponseEntity<StudyResponse> createStudy(@Valid @RequestBody StudyCreateRequest request) {
    return ResponseEntity.ok(StudyResponse.builder()
            .roomId(request.getRoomId())
            .title(request.getTitle())
            .description(request.getDescription())
            .category(request.getCategory())
            .maxParticipant(request.getMaxParticipant())
            .status(StudyStatus.ACTIVE)
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .build());
  }
}


