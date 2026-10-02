package com.binteum.domain.participation.entity;

import com.binteum.domain.participation.enums.ParticipationStatus;
import com.binteum.domain.study.entity.Study;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "participation",
    uniqueConstraints =
    @UniqueConstraint(
        name = "uk_participation_study_user",
        columnNames = {"study_id", "user_id"}))
public class Participation extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "participation_id")
  private Long participationId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "study_id", nullable = false)
  private Study study;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 10, nullable = false)
  private ParticipationStatus status = ParticipationStatus.JOINED;

  @Column(name = "joined_at")
  private LocalDateTime joinedAt;

  public static Participation join(User user, Study study) {
    Participation participation = new Participation();
    participation.user = user;
    participation.study = study;
    participation.status = ParticipationStatus.JOINED;
    participation.joinedAt = LocalDateTime.now();
    return participation;
  }
}




