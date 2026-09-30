package com.binteum.domain.study.dto;

import com.binteum.domain.study.enums.StudyCategory;
import com.binteum.domain.study.enums.StudyStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class StudyResponse {

  private Long studyId;
  private Long hostId;
  private Long roomId;
  private String title;
  private String description;
  private StudyCategory category;
  private Integer maxParticipant;
  private Long currentParticipants;
  private StudyStatus status;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
}


