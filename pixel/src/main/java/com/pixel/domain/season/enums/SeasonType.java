package com.pixel.domain.season.enums;

import java.time.LocalDate;
import java.util.Objects;

public enum SeasonType {
  SPRING,
  SUMMER,
  AUTUMN,
  WINTER;

  /** 촬영 연도가 아니라 월로만 계절을 결정한다. 겨울은 12·1·2월이다. */
  public static SeasonType fromDate(LocalDate date) {
    int month = Objects.requireNonNull(date, "date").getMonthValue();
    if (month >= 3 && month <= 5) return SPRING;
    if (month >= 6 && month <= 8) return SUMMER;
    if (month >= 9 && month <= 11) return AUTUMN;
    return WINTER;
  }
}
