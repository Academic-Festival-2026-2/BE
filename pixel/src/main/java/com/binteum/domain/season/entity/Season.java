package com.binteum.domain.season.entity;

import com.binteum.domain.season.enums.SeasonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "seasons")
public class Season {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "season_id")
  private Short seasonId;

  @Column(name = "year", nullable = false)
  private Short year;

  @Enumerated(EnumType.STRING)
  @Column(name = "season", length = 6, nullable = false)
  private SeasonType season;

  @Column(name = "starts_on", nullable = false)
  private LocalDate startsOn;

  @Column(name = "ends_on", nullable = false)
  private LocalDate endsOn;
}
