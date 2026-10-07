package com.binteum.domain.study.repository;

import com.binteum.domain.study.entity.Study;
import com.binteum.domain.study.enums.StudyCategory;
import com.binteum.domain.study.enums.StudyStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  @Query(value = "select s from Study s "
      + "join fetch s.classroom c "
      + "join fetch c.building b "
      + "where s.status = :status "
      + "and s.endTime > :now "
      + "and s.startTime >= :from and s.startTime < :to "
      + "and (:keyword is null or s.title like concat('%', :keyword, '%')) "
      + "and (:buildingId is null or b.buildingId = :buildingId) "
      + "and (:category is null or s.category = :category)",
      countQuery = "select count(s) from Study s "
          + "join s.classroom c "
          + "join c.building b "
          + "where s.status = :status "
          + "and s.endTime > :now "
          + "and s.startTime >= :from and s.startTime < :to "
          + "and (:keyword is null or s.title like concat('%', :keyword, '%')) "
          + "and (:buildingId is null or b.buildingId = :buildingId) "
          + "and (:category is null or s.category = :category)")
  Page<Study> findList(
      @Param("status") StudyStatus status,
      @Param("now") LocalDateTime now,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("keyword") String keyword,
      @Param("buildingId") Long buildingId,
      @Param("category") StudyCategory category,
      Pageable pageable);
}