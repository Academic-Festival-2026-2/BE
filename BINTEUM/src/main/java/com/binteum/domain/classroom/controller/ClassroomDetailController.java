package com.binteum.domain.classroom.controller;

import com.binteum.domain.classroom.dto.ClassroomDetailResponse;
import com.binteum.domain.classroom.service.ClassroomService;
import com.binteum.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/classrooms")
public class ClassroomDetailController {

  private final ClassroomService classroomService;

  @GetMapping("/{roomId}")
  public ResponseEntity<ApiResponse<ClassroomDetailResponse>> getClassroomDetail(
      @PathVariable("roomId") Long roomId) {
    return ResponseEntity.ok(
        ApiResponse.onSuccess(classroomService.getClassroomDetail(roomId)));
  }
}