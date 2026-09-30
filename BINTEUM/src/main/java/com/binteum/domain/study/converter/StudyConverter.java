package com.binteum.domain.study.converter;

import com.binteum.domain.classroom.entity.Classroom;
import com.binteum.domain.study.dto.StudyCreateRequest;
import com.binteum.domain.study.dto.StudyResponse;
import com.binteum.domain.study.entity.Study;
import com.binteum.domain.user.entity.User;

public class StudyConverter {

  private StudyConverter() {
  }

  public static Study toStudy(StudyCreateRequest request, Classroom classroom, User host) {
    return Study.builder()
        .classroom(classroom)
        .host(host)
        .title(request.getTitle())
        .description(request.getDescription())
        .category(request.getCategory())
        .maxParticipant(request.getMaxParticipant())
        .startTime(request.getStartTime())
        .endTime(request.getEndTime())
        .build();
  }

  public static StudyResponse toStudyResponse(Study study, long currentParticipants) {
    return StudyResponse.builder()
        .studyId(study.getStudyId())
        .hostId(study.getHost().getUserId())
        .roomId(study.getClassroom().getRoomId())
        .title(study.getTitle())
        .description(study.getDescription())
        .category(study.getCategory())
        .maxParticipant(study.getMaxParticipant())
        .startTime(study.getStartTime())
        .endTime(study.getEndTime())
        .status(study.getStatus())
        .currentParticipants(currentParticipants)
        .build();
  }
}
