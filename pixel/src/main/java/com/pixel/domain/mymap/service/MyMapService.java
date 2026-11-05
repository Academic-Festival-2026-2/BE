package com.pixel.domain.mymap.service;

import com.pixel.domain.mymap.dto.MyMapResponse;
import com.pixel.domain.season.enums.SeasonType;

public interface MyMapService {

  MyMapResponse getMyMap(Long userId, SeasonType season);
}
