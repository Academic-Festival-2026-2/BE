package com.binteum.domain.feedback.controller;

import com.binteum.domain.feedback.dto.FeedbackCreateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

  @PostMapping
  public ResponseEntity<String> createFeedback(@Valid @RequestBody FeedbackCreateRequest request) {
    return ResponseEntity.ok("피드백이 등록되었습니다.");
  }
}


