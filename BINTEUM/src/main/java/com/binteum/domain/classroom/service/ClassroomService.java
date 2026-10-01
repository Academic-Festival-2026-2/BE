package com.binteum.domain.classroom.service;

import com.binteum.domain.building.repository.BuildingRepository;
import com.binteum.domain.classroom.dto.ClassroomResponse;
import com.binteum.domain.classroom.repository.ClassroomRepository;
import com.binteum.global.apiPayload.exception.GeneralException;
import com.binteum.global.apiPayload.status.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.binteum.domain.classroom.dto.ClassroomDetailResponse;
import com.binteum.domain.classschedule.repository.ClassScheduleRepository;
import java.time.LocalDate;
import java.time.ZoneId;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassroomService {

    private final BuildingRepository buildingRepository;
    private final ClassroomRepository classroomRepository;
    private final ClassScheduleRepository classScheduleRepository;

    public List<ClassroomResponse> getClassrooms(Long buildingId) {
        if (!buildingRepository.existsById(buildingId)) {
            throw new GeneralException(ErrorStatus.BUILDING_NOT_FOUND);
        }

        return classroomRepository
                .findByBuilding_BuildingId(buildingId)
                .stream()
                .map(classroom -> new ClassroomResponse(
                        classroom.getRoomId(),
                        classroom.getRoomNumber(),
                        classroom.getCapacity(),
                        classroom.getHasOutlet()
                ))
                .toList();
    }

    public ClassroomDetailResponse getClassroomDetail(Long roomId) {
        var classroom = classroomRepository.findById(roomId)
                .orElseThrow(() ->
                        new GeneralException(ErrorStatus.CLASSROOM_NOT_FOUND)
                );

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        String dayOfWeek = today.getDayOfWeek().name();

        var schedules = classScheduleRepository
                .findByClassroom_RoomIdAndDayOfWeekOrderByStartTimeAsc(
                        roomId, dayOfWeek
                )
                .stream()
                .map(schedule ->
                        new ClassroomDetailResponse.ScheduleResponse(
                                schedule.getStartTime(),
                                schedule.getEndTime(),
                                schedule.getCourseName()
                        )
                )
                .toList();

        var building = classroom.getBuilding();

        return new ClassroomDetailResponse(
                classroom.getRoomId(),
                classroom.getRoomNumber(),
                building.getBuildingId(),
                building.getName(),
                today,
                dayOfWeek,
                schedules
        );
    }
}