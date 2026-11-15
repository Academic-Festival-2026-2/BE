package com.pixel.domain.photo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.season.enums.SeasonType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PhotoUploadRequest(
    String dongCode,
    Double latitude,
    Double longitude,
    @JsonFormat(pattern = "uuuu-MM-dd'T'HH:mm:ss", lenient = OptBoolean.FALSE)
    LocalDateTime takenAt,
    LocalDate shotDate,
    Boolean isPublic,
    List<String> palette,
    List<Double> bins,
    SeasonType season,
    TimeBand timeBand) {

  // 기존 Java 코드에서 8개 인자를 전달하는 호출을 유지한다.
  public PhotoUploadRequest(
      String dongCode,
      Double latitude,
      Double longitude,
      LocalDateTime takenAt,
      LocalDate shotDate,
      Boolean isPublic,
      List<String> palette,
      List<Double> bins) {
    this(dongCode, latitude, longitude, takenAt, shotDate, isPublic, palette, bins, null, null);
  }
}
