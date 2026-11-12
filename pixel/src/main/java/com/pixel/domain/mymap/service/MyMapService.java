package com.pixel.domain.mymap.service;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.mymap.dto.MyDongPhotosResponse;
import com.pixel.domain.mymap.dto.MyMapResponse;
import com.pixel.domain.mymap.dto.MyMapShareResponse;
import com.pixel.domain.mymap.dto.SharedMapResponse;
import com.pixel.domain.season.enums.SeasonType;

public interface MyMapService {

  MyMapResponse getMyMap(Long userId, SeasonType season, String regionCode);

  MyDongPhotosResponse getMyDongPhotos(Long userId, String dongCode, TimeBand timeBand,
      SeasonType season);

  MyMapShareResponse shareMyMap(Long userId);

  void stopSharingMyMap(Long userId);

  SharedMapResponse getSharedMap(String shareKey, SeasonType season, String regionCode);

  MyMapShareResponse getMyShare(Long userId);

  /** 나의 지도의 filledCellCount와 같은 기준으로 채운 칸 수만 센다. 시즌이 없으면 0. */
  int countFilledCells(Long userId, SeasonType season);
}
