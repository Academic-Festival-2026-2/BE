package com.binteum.domain.study.repository;

import com.binteum.domain.study.entity.Study;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudyRepository extends JpaRepository<Study, Long> {

  @Query("select s from Study s "
      + "join fetch s.host "
      + "join fetch s.classroom c "
      + "join fetch c.building "
      + "where s.studyId = :studyId")
  Optional<Study> findDetailById(@Param("studyId") Long studyId);
}


