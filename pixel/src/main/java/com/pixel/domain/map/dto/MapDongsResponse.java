package com.pixel.domain.map.dto;

import com.pixel.domain.colorcell.enums.CellStatus;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.season.enums.SeasonType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MapDongsResponse {

  private final SeasonType season;
  private final TimeBand timeBand;
  private final List<DongColor> dongs;

  @Getter
  @AllArgsConstructor
  public static class DongColor {

    private final String dongCode;
    private final String name;
    private final String dominantColor;
    private final int photoCount;
    private final CellStatus status;
  }
}
