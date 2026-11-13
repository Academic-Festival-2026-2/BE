package com.pixel.domain.map.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.pixel.domain.map.dto.RegionListResponse;
import com.pixel.domain.region.dto.RegionBoundsView;
import com.pixel.domain.region.repository.RegionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapServiceRegionsTest {

  @Mock
  private RegionRepository regionRepository;

  @InjectMocks
  private MapServiceImpl mapService;

  @Test
  void regionsHaveBboxInLngLatOrderAndDefaultRegion() {
    // 네 값을 모두 다르게 넣어야 bbox 순서가 바뀌는 실수를 잡는다
    RegionBoundsView view = mock(RegionBoundsView.class);
    given(view.getRegionCode()).willReturn("3105");
    given(view.getName()).willReturn("부천시");
    given(view.getDongCount()).willReturn(37);
    given(view.getMinLng()).willReturn(126.74);
    given(view.getMinLat()).willReturn(37.46);
    given(view.getMaxLng()).willReturn(126.84);
    given(view.getMaxLat()).willReturn(37.55);
    given(regionRepository.findAllWithBounds()).willReturn(List.of(view));

    RegionListResponse response = mapService.getRegions();

    assertThat(response.defaultRegionCode()).isEqualTo("3105");
    assertThat(response.regions()).hasSize(1);
    RegionListResponse.Item item = response.regions().get(0);
    assertThat(item.regionCode()).isEqualTo("3105");
    assertThat(item.name()).isEqualTo("부천시");
    assertThat(item.dongCount()).isEqualTo(37);
    assertThat(item.bbox()).containsExactly(126.74, 37.46, 126.84, 37.55);
  }

  @Test
  void emptyWhenNoRegionHasDongs() {
    given(regionRepository.findAllWithBounds()).willReturn(List.of());

    RegionListResponse response = mapService.getRegions();

    assertThat(response.defaultRegionCode()).isEqualTo("3105");
    assertThat(response.regions()).isEmpty();
  }
}
