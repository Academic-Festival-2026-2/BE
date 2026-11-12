package com.pixel.domain.region.service;

import com.pixel.domain.region.entity.Region;
import com.pixel.domain.region.exception.code.RegionErrorCode;
import com.pixel.domain.region.repository.RegionRepository;
import com.pixel.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionServiceImpl implements RegionService {

  private final RegionRepository regionRepository;

  @Override
  public Region getRegion(String regionCode) {
    return regionRepository.findById(regionCode)
        .orElseThrow(() -> new GeneralException(RegionErrorCode._REGION_NOT_FOUND));
  }
}
