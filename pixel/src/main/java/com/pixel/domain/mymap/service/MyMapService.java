package com.pixel.domain.mymap.service;

import com.pixel.domain.mymap.dto.MyMapResponse;
import com.pixel.domain.season.enums.SeasonType;

public interface MyMapService {

  MyMapResponse getMyMap(Long userId, SeasonType season);

  /** 나의 지도의 filledCellCount와 같은 기준으로 채운 칸 수만 센다. 시즌이 없으면 0. */
  int countFilledCells(Long userId, SeasonType season);
}
