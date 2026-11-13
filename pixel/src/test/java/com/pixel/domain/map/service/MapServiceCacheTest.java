package com.pixel.domain.map.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.dong.repository.DongRepository;
import com.pixel.domain.region.dto.RegionBoundsView;
import com.pixel.domain.region.repository.RegionRepository;
import com.pixel.domain.region.service.RegionService;
import com.pixel.domain.season.service.SeasonService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

// 서버 전체를 띄우지 않고 MapServiceImpl과 캐시 설정만 올려서 @Cacheable이 실제로 동작하는지 본다
@SpringJUnitConfig({MapServiceImpl.class, MapServiceCacheTest.CacheConfig.class})
class MapServiceCacheTest {

  @Configuration
  @EnableCaching
  static class CacheConfig {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager();
    }
  }

  @Autowired
  private CacheManager cacheManager;

  @BeforeEach
  void clearCache() {
    cacheManager.getCache("regions").clear();
  }

  @MockitoBean
  private RegionRepository regionRepository;

  @MockitoBean
  private DongRepository dongRepository;

  @MockitoBean
  private ColorCellRepository colorCellRepository;

  @MockitoBean
  private SeasonService seasonService;

  @MockitoBean
  private RegionService regionService;

  @Autowired
  private MapService mapService;

  @Test
  void regionsAreCalculatedOnlyOnce() {
    RegionBoundsView bucheon = mock(RegionBoundsView.class);
    given(bucheon.getRegionCode()).willReturn("3105");
    given(regionRepository.findAllWithBounds()).willReturn(List.of(bucheon));

    mapService.getRegions();
    mapService.getRegions();

    verify(regionRepository, times(1)).findAllWithBounds();
  }

  @Test
  void emptyRegionsAreNotCached() {
    // 경계를 아직 안 넣은 서버: 빈 목록은 저장하지 않아서, 경계를 넣으면 재시작 없이 다음 요청에 반영된다
    given(regionRepository.findAllWithBounds()).willReturn(List.of());

    mapService.getRegions();
    mapService.getRegions();

    verify(regionRepository, times(2)).findAllWithBounds();
  }
}
