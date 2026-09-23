package com.binteum.domain.study.dto;

import com.binteum.domain.study.enums.StudyStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudyResponse {
  private Long studyId;
  private Long hostId;
  private String title;
  private String category;
  private Integer maxParticipant;
  private StudyStatus status;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
}


