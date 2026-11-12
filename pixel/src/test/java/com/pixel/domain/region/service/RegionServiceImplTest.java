package com.pixel.domain.region.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.pixel.domain.region.exception.code.RegionErrorCode;
import com.pixel.domain.region.repository.RegionRepository;
import com.pixel.global.apiPayload.exception.GeneralException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RegionServiceImplTest {

  @Mock
  private RegionRepository regionRepository;

  @InjectMocks
  private RegionServiceImpl regionService;

  @Test
  void unknownRegionCodeThrowsNotFound() {
    given(regionRepository.findById("9999")).willReturn(Optional.empty());

    GeneralException ex = assertThrows(GeneralException.class,
        () -> regionService.getRegion("9999"));

    assertEquals(RegionErrorCode._REGION_NOT_FOUND, ex.getCode());
  }
}
