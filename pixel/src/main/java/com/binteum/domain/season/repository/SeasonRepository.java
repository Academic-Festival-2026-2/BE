package com.binteum.domain.season.repository;

import com.binteum.domain.season.entity.Season;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeasonRepository extends JpaRepository<Season, Short> {

  @Query("SELECT s FROM Season s WHERE :today BETWEEN s.startsOn AND s.endsOn")
  Optional<Season> findCurrent(@Param("today") LocalDate today);
}
