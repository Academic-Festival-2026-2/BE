package com.binteum.domain.study.controller;

import com.binteum.domain.study.dto.StudyCreateRequest;
import com.binteum.domain.study.dto.StudyResponse;
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
    return ResponseEntity.ok(new StudyResponse(
        studyId,
        1L,
        "알고리즘 스터디",
        "코딩",
        5,
        com.binteum.domain.study.enums.StudyStatus.RECRUITING,
        LocalDateTime.now(),
        LocalDateTime.now().plusHours(2)
    ));
  }

  @PostMapping
  public ResponseEntity<StudyResponse> createStudy(@Valid @RequestBody StudyCreateRequest request) {
    return ResponseEntity.ok(new StudyResponse(
        1L,
        1L,
        request.getTitle(),
        request.getCategory(),
        request.getMaxParticipant(),
        com.binteum.domain.study.enums.StudyStatus.RECRUITING,
        request.getStartTime(),
        request.getEndTime()
    ));
  }
}


