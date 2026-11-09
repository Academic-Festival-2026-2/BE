package com.pixel.domain.mymap.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.dong.repository.DongRepository;
import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.season.service.SeasonService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyMapFilledCellCountTest {

  private static final Long USER_ID = 1L;

  @Mock
  private SeasonService seasonService;

  @Mock
  private DongRepository dongRepository;

  @Mock
  private PhotoRepository photoRepository;

  @Mock
  private ColorCellRepository colorCellRepository;

  @InjectMocks
  private MyMapServiceImpl myMapService;

  @Test
  void 같은_칸의_사진은_한_칸으로_센다() {
    Season season = mock(Season.class);
    Dong dongA = dong("A");
    Dong dongB = dong("B");
    // mock 생성이 다른 스텁 도중에 일어나지 않도록 사진 목록을 먼저 만든다
    List<Photo> photos = List.of(
        photo(dongA, TimeBand.DAY),
        photo(dongA, TimeBand.DAY),
        photo(dongA, TimeBand.NIGHT),
        photo(dongB, TimeBand.DAY));
    given(seasonService.findSeason(SeasonType.AUTUMN)).willReturn(Optional.of(season));
    given(photoRepository.findMyMapPhotos(USER_ID, season)).willReturn(photos);

    int count = myMapService.countFilledCells(USER_ID, SeasonType.AUTUMN);

    assertThat(count).isEqualTo(3);
    // 칸 수만 셀 때는 동 목록과 모두의 지도 색을 조회하지 않는다
    verifyNoInteractions(dongRepository, colorCellRepository);
  }

  @Test
  void 시즌이_없으면_0을_돌려준다() {
    given(seasonService.findSeason(SeasonType.AUTUMN)).willReturn(Optional.empty());

    int count = myMapService.countFilledCells(USER_ID, SeasonType.AUTUMN);

    assertThat(count).isZero();
    verify(photoRepository, never()).findMyMapPhotos(any(), any());
  }

  private Dong dong(String dongCode) {
    Dong dong = mock(Dong.class);
    given(dong.getDongCode()).willReturn(dongCode);

    return dong;
  }

  private Photo photo(Dong dong, TimeBand timeBand) {
    Photo photo = mock(Photo.class);
    given(photo.getDong()).willReturn(dong);
    given(photo.getTimeBand()).willReturn(timeBand);

    return photo;
  }
}
