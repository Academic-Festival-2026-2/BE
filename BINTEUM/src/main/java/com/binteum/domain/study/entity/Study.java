package com.binteum.domain.study.entity;

import com.binteum.domain.classroom.entity.Classroom;
import com.binteum.domain.study.enums.StudyStatus;
import com.binteum.domain.user.entity.User;
import jakarta.persistence.*;

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "room_id", nullable = false)
  private Classroom classroom;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "host_id", nullable = false)
  private User host;

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




