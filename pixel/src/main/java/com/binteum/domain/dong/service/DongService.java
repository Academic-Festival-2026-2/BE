package com.binteum.domain.dong.service;

import com.binteum.domain.colorcell.enums.TimeBand;
import com.binteum.domain.dong.dto.DongDetailResponse;

public interface DongService {

  DongDetailResponse getDetail(String dongCode, TimeBand timeBand);
}
