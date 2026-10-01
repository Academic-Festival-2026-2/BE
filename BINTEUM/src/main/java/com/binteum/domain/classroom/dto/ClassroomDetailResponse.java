package com.binteum.domain.classroom.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ClassroomDetailResponse(
        Long roomId,
        String roomNumber,
        Long buildingId,
        String buildingName,
        LocalDate date,
        String dayOfWeek,
        List<ScheduleResponse> schedules
) {

    public record ScheduleResponse(
            LocalTime startTime,
            LocalTime endTime,
            String courseName
    ) {
    }
}