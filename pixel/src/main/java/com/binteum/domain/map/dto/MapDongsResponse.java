package com.binteum.domain.map.dto;

import com.binteum.domain.colorcell.enums.CellStatus;
import com.binteum.domain.colorcell.enums.TimeBand;
import com.binteum.domain.season.enums.SeasonType;
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
