package com.binteum.domain.study.dto;

import com.binteum.domain.study.enums.StudyCategory;
import com.binteum.domain.study.enums.StudyDisplayStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudyDetailResponse {

  //스터디 기본 정보
  private Long studyId;
  private String title;
  private String description;
  private StudyCategory category;
  private LocalDateTime startTime;
  private LocalDateTime endTime;

  //인원,상태
  private Integer maxParticipant;
  private Long currentParticipants;
  private StudyDisplayStatus displayStatus;

  //호스트
  private Long hostId;
  private String hostNickname;

  //장소
  private Long buildingId;
  private String buildingName;
  private Long roomId;
  private String roomNumber;

  //보는사람기준
  private Boolean isHost;
  private Boolean isJoined;
}
