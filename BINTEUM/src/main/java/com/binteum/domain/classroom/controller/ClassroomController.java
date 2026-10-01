package com.binteum.domain.classroom.controller;

import com.binteum.domain.classroom.dto.ClassroomResponse;
import com.binteum.domain.classroom.service.ClassroomService;
import com.binteum.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/buildings")
public class ClassroomController {

    private final ClassroomService classroomService;

    @GetMapping("/{buildingId}/classrooms")
    public ApiResponse<List<ClassroomResponse>> getClassrooms(
            @PathVariable("buildingId") Long buildingId
    ) {
        return ApiResponse.onSuccess(
                classroomService.getClassrooms(buildingId)
        );
    }
}