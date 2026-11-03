package com.pixel.domain.dong.service;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.dong.dto.DongDetailResponse;

public interface DongService {

  DongDetailResponse getDetail(String dongCode, TimeBand timeBand);
}
