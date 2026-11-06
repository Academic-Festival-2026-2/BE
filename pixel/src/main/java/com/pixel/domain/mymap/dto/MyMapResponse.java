package com.pixel.domain.mymap.dto;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.season.enums.SeasonType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyMapResponse {

  private final SeasonType season;
  private final int totalCellCount;
  private final int filledCellCount;
  private final List<Cell> cells;

  @Getter
  @AllArgsConstructor
  public static class Cell {

    private final String dongCode;
    private final String name;
    private final TimeBand timeBand;
    private final boolean filled;
    private final String myColor;
    private final int myPhotoCount;
    private final String previewColor;
  }
}
