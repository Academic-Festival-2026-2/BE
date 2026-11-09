package com.pixel.domain.mymap.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.dong.dto.DongDetailResponse;
import com.pixel.domain.dong.repository.DongRepository;
import com.pixel.domain.mymap.dto.MyDongPhotosResponse;
import com.pixel.domain.photo.dto.PhotoPointView;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.season.service.SeasonService;
import com.pixel.global.apiPayload.exception.GeneralException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyDongPhotosTest {

  private static final Long USER_ID = 1L;
  private static final String DONG = "31051581";

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
  void returnsMyPhotosIncludingPhotoWithoutLocation() {
    Season season = mock(Season.class);
    // mock 생성이 다른 설정 도중에 일어나지 않도록 목록을 먼저 만든다
    List<PhotoPointView> views = List.of(view(9L, 37.48632, 126.80163), view(12L, null, null));
    given(season.getSeasonId()).willReturn((short) 1);
    given(dongRepository.existsById(DONG)).willReturn(true);
    given(seasonService.getSeason(SeasonType.AUTUMN)).willReturn(season);
    given(photoRepository.findMyPhotoPoints(USER_ID, DONG, (short) 1, "SUNSET")).willReturn(views);

    MyDongPhotosResponse response =
        myMapService.getMyDongPhotos(USER_ID, DONG, TimeBand.SUNSET, SeasonType.AUTUMN);

    assertThat(response.points())
        .extracting(DongDetailResponse.Point::getPhotoId)
        .containsExactly(9L, 12L);
    assertThat(response.points().get(1).getLat()).isNull();
  }

  @Test
  void failsWhenDongDoesNotExist() {
    given(dongRepository.existsById("NONE")).willReturn(false);

    assertThatThrownBy(() -> myMapService.getMyDongPhotos(USER_ID, "NONE", null, SeasonType.AUTUMN))
        .isInstanceOf(GeneralException.class);
    verifyNoInteractions(photoRepository);
  }

  private PhotoPointView view(Long photoId, Double lat, Double lng) {
    PhotoPointView view = mock(PhotoPointView.class);
    given(view.getPhotoId()).willReturn(photoId);
    given(view.getLat()).willReturn(lat);
    given(view.getLng()).willReturn(lng);
    given(view.getColor()).willReturn("#E5762F");
    given(view.getTimeBand()).willReturn("SUNSET");
    return view;
  }
}
