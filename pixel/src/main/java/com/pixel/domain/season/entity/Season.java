package com.pixel.domain.season.entity;

import com.pixel.domain.season.enums.SeasonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 계절당 한 행. 연도별 시즌은 만들지 않는다. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "seasons")
public class Season {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "season_id")
  private Short seasonId;

  @Enumerated(EnumType.STRING)
  @Column(name = "season", length = 6, nullable = false, unique = true)
  private SeasonType season;
}
