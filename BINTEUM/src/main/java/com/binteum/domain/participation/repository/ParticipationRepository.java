package com.binteum.domain.participation.repository;

import com.binteum.domain.participation.entity.Participation;
import com.binteum.domain.participation.enums.ParticipationStatus;
import com.binteum.domain.study.entity.Study;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

  long countByStudyAndStatus(Study study, ParticipationStatus status);

  boolean existsByStudyAndUser_UserIdAndStatus(Study study, Long userId,
      ParticipationStatus status);

  @Query("select p.study.studyId as studyId, count(p) as count "
      + "from Participation p "
      + "where p.study.studyId in :studyIds and p.status = :status "
      + "group by p.study.studyId")
  List<StudyParticipantCount> countByStudyIds(
      @Param("studyIds") List<Long> studyIds,
      @Param("status") ParticipationStatus status);
}
