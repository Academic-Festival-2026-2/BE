package com.binteum.domain.participation.repository;

import com.binteum.domain.participation.entity.Participation;
import com.binteum.domain.participation.enums.ParticipationStatus;
import com.binteum.domain.study.entity.Study;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

  long countByStudyAndStatus(Study study, ParticipationStatus status);

  boolean existsByStudyAndUser_UserIdAndStatus(Study study, Long userId,
      ParticipationStatus status);
}
