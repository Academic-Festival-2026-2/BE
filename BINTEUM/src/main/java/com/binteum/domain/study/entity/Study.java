package com.binteum.domain.study.entity;

import com.binteum.domain.study.enums.StudyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.binteum.global.common.BaseEntity;@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "study")
public class Study extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "study_id")
  private Long studyId;

  @Column(name = "room_id")
  private Long roomId;

  @Column(name = "host_id", nullable = false)
  private Long hostId;

  @Column(name = "title", length = 100, nullable = false)
  private String title;

  @Column(name = "category", length = 30, nullable = false)
  private String category;

  @Column(name = "max_participant", nullable = false)
  private Integer maxParticipant;

  @Column(name = "status", length = 20, nullable = false)
  @Enumerated(EnumType.STRING)
  private StudyStatus status = StudyStatus.RECRUITING;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalDateTime endTime;
}




