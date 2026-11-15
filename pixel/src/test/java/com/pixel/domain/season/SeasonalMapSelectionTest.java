package com.pixel.domain.season;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.dong.repository.DongRepository;
import com.pixel.domain.dong.service.DongServiceImpl;
import com.pixel.domain.map.service.MapServiceImpl;
import com.pixel.domain.mymap.service.MyMapServiceImpl;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.region.entity.Region;
import com.pixel.domain.region.repository.RegionRepository;
import com.pixel.domain.region.service.RegionService;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.season.service.SeasonService;
import com.pixel.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeasonalMapSelectionTest {

  @Mock DongRepository dongRepository;
  @Mock ColorCellRepository colorCellRepository;
  @Mock PhotoRepository photoRepository;
  @Mock SeasonService seasonService;
  @Mock RegionService regionService;
  @Mock RegionRepository regionRepository;
  @Mock UserRepository userRepository;

  @InjectMocks MapServiceImpl mapService;
  @InjectMocks DongServiceImpl dongService;
  @InjectMocks MyMapServiceImpl myMapService;

  @Test
  void sharedMapSelectsSpringAndReturnsEmptyData() {
    Season spring = mock(Season.class);
    Region region = mock(Region.class);

    given(seasonService.getSeason(SeasonType.SPRING)).willReturn(spring);
    given(spring.getSeason()).willReturn(SeasonType.SPRING);
    given(regionService.getRegion("3105")).willReturn(region);
    given(region.getRegionCode()).willReturn("3105");

    var result = mapService.getDongs(TimeBand.DAY, "3105", SeasonType.SPRING);

    assertThat(result.getSeason()).isEqualTo(SeasonType.SPRING);
    assertThat(result.getDongs()).isEmpty();
    assertThat(result.getTopDongs()).isEmpty();
    verify(colorCellRepository).findForMap(spring, TimeBand.DAY, "3105");
  }

  @Test
  void dongDetailUsesSelectedSeasonForColorsAndPoints() {
    String code = "31050110";
    Dong dong = mock(Dong.class);
    Region region = mock(Region.class);
    Season summer = mock(Season.class);

    given(dongRepository.findById(code)).willReturn(Optional.of(dong));
    given(dong.getRegion()).willReturn(region);
    given(seasonService.getSeason(SeasonType.SUMMER)).willReturn(summer);
    given(summer.getSeasonId()).willReturn((short) 2);
    given(summer.getSeason()).willReturn(SeasonType.SUMMER);

    var result = dongService.getDetail(code, TimeBand.DAY, SeasonType.SUMMER);

    assertThat(result.getSeason()).isEqualTo(SeasonType.SUMMER);
    assertThat(result.getPoints()).isEmpty();
    verify(colorCellRepository).findByDongAndSeason(dong, summer);
    verify(photoRepository).findPhotoPoints(code, (short) 2, "DAY");
  }

  @Test
  void myMapWithoutSeasonUsesCurrentSeason() {
    Season winter = mock(Season.class);
    Region region = mock(Region.class);

    given(seasonService.getCurrentSeason()).willReturn(winter);
    given(winter.getSeason()).willReturn(SeasonType.WINTER);
    given(regionService.getRegion("3105")).willReturn(region);
    given(region.getRegionCode()).willReturn("3105");

    var result = myMapService.getMyMap(1L, null, "3105");

    assertThat(result.getSeason()).isEqualTo(SeasonType.WINTER);
    verify(photoRepository).findMyMapPhotos(1L, winter);
  }
}
