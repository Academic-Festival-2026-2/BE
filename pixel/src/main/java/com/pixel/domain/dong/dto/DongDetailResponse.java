package com.pixel.domain.dong.dto;

import com.pixel.domain.colorcell.dto.ColorRatio;
import com.pixel.domain.colorcell.enums.CellStatus;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.season.enums.SeasonType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DongDetailResponse {

  private final String dongCode;
  private final String name;
  private final String guName;
  private final SeasonType season;
  private final List<TimeBandSummary> timeBands;
  private final List<Point> points;

  @Getter
  @AllArgsConstructor
  public static class TimeBandSummary {

    private final TimeBand timeBand;
    private final String dominantColor;
    private final int photoCount;
    private final CellStatus status;
    private final List<ColorRatio> colorRatio;
  }

  @Getter
  @AllArgsConstructor
  public static class Point {

    private final Long photoId;
    private final Double lat;
    private final Double lng;
    private final String color;
    private final TimeBand timeBand;
  }
}
