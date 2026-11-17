package com.pixel.domain.dong.service;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.dong.dto.DongDetailResponse;
import com.pixel.domain.season.enums.SeasonType;

public interface DongService {

  DongDetailResponse getDetail(String dongCode, TimeBand timeBand, SeasonType season);
}
