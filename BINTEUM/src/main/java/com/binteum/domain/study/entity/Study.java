package com.binteum.domain.study.entity;

import com.binteum.domain.classroom.entity.Classroom;
import com.binteum.domain.study.enums.StudyCategory;
import com.binteum.domain.study.enums.StudyDisplayStatus;
import com.binteum.domain.study.enums.StudyStatus;
import com.binteum.domain.user.entity.User;
import com.binteum.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
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

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "category", columnDefinition = "VARCHAR(30)", nullable = false)
  @Enumerated(EnumType.STRING)
  private StudyCategory category;

  @Column(name = "max_participant", nullable = false)
  private Integer maxParticipant;

  @Column(name = "status", length = 20, nullable = false)
  @Enumerated(EnumType.STRING)
  private StudyStatus status = StudyStatus.ACTIVE;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalDateTime endTime;

  @Builder
  private Study(
      Classroom classroom,
      User host,
      String title,
      String description,
      StudyCategory category,
      Integer maxParticipant,
      LocalDateTime startTime,
      LocalDateTime endTime) {
    this.classroom = classroom;
    this.host = host;
    this.title = title;
    this.description = description;
    this.category = category;
    this.maxParticipant = maxParticipant;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public StudyDisplayStatus calculateDisplayStatus(long currentParticipants, LocalDateTime now) {
    if (status == StudyStatus.CANCELLED) {
      return StudyDisplayStatus.CANCELLED;
    }
    if (!endTime.isAfter(now)) {
      return StudyDisplayStatus.COMPLETED;
    }
    if (!startTime.isAfter(now)) {
      return StudyDisplayStatus.IN_PROGRESS;
    }
    if (currentParticipants >= maxParticipant) {
      return StudyDisplayStatus.FULL;
    }
    return StudyDisplayStatus.RECRUITING;
  }
}




