package com.binteum.domain.study.dto;

import com.binteum.domain.study.enums.StudyCategory;
import com.binteum.domain.study.enums.StudyDisplayStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudySummaryResponse {

  //스터디 기본 정보
  private Long studyId;
  private StudyCategory category;
  private String title;

  //시간
  private LocalDateTime startTime;
  private LocalDateTime endTime;

  //인원
  private Long currentParticipants;
  private Integer maxParticipant;

  //상태
  private StudyDisplayStatus displayStatus;

  //건물
  private String buildingName;
  private String roomNumber;
}
