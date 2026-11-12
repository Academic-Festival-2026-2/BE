package com.pixel.domain.mymap.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.pixel.domain.colorcell.entity.ColorCell;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.colorcell.util.ColorBins;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.dong.repository.DongRepository;
import com.pixel.domain.mymap.dto.MyMapResponse;
import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.region.entity.Region;
import com.pixel.domain.region.service.RegionService;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.season.service.SeasonService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyMapServiceImplTest {

  private static final Long USER_ID = 1L;

  @Mock
  private SeasonService seasonService;

  @Mock
  private DongRepository dongRepository;

  @Mock
  private PhotoRepository photoRepository;

  @Mock
  private ColorCellRepository colorCellRepository;

  @Mock
  private RegionService regionService;

  @InjectMocks
  private MyMapServiceImpl myMapService;

  private final Season season = mock(Season.class);
  private final Region region = mock(Region.class);
  private Dong dongA;
  private Dong dongB;

  @BeforeEach
  void setUp() {
    given(seasonService.getSeason(SeasonType.AUTUMN)).willReturn(season);

    given(season.getSeason()).willReturn(SeasonType.AUTUMN);

    given(regionService.getRegion("3105")).willReturn(region);
    given(region.getRegionCode()).willReturn("3105");

    dongA = dong("A");
    dongB = dong("B");

    given(dongRepository.findByRegionRegionCodeOrderByDongCode("3105"))
        .willReturn(List.of(dongA, dongB));
  }

  @Test
  void emptyWhenNoPhoto() {
    givenPhotos();
    givenColorCells();

    MyMapResponse response = myMapService.getMyMap(USER_ID, SeasonType.AUTUMN, "3105");

    assertThat(response.getSeason()).isEqualTo(SeasonType.AUTUMN);
    assertThat(response.getTotalCellCount()).isEqualTo(8);
    assertThat(response.getFilledCellCount()).isZero();
    assertThat(response.getCells())
        .extracting(MyMapResponse.Cell::getTimeBand)
        .containsExactly(TimeBand.DAWN, TimeBand.DAY, TimeBand.SUNSET, TimeBand.NIGHT,
            TimeBand.DAWN, TimeBand.DAY, TimeBand.SUNSET, TimeBand.NIGHT);
  }

  @Test
  void fillsOnlyCellsWithMyPhoto() {
    givenPhotos(photo(dongA, TimeBand.DAY, bins(Map.of(12, 1.0))));
    givenColorCells();

    MyMapResponse response = myMapService.getMyMap(USER_ID, SeasonType.AUTUMN, "3105");

    MyMapResponse.Cell filled = cellOf(response, "A", TimeBand.DAY);
    assertThat(filled.isFilled()).isTrue();
    assertThat(filled.getMyPhotoCount()).isEqualTo(1);
    assertThat(cellOf(response, "A", TimeBand.NIGHT).isFilled()).isFalse();

    assertThat(response.getFilledCellCount()).isEqualTo(1);
  }

  @Test
  void sumsBinsOfMyPhotosForMyColor() {
    givenPhotos(
        photo(dongA, TimeBand.DAY, bins(Map.of(12, 1.0))),
        photo(dongA, TimeBand.DAY, bins(Map.of(0, 0.6, 12, 0.4))));

    givenColorCells();

    MyMapResponse response = myMapService.getMyMap(USER_ID, SeasonType.AUTUMN, "3105");

    MyMapResponse.Cell cell = cellOf(response, "A", TimeBand.DAY);

    assertThat(cell.getMyPhotoCount()).isEqualTo(2);
    assertThat(cell.getMyColor()).isEqualTo(ColorBins.HEX.get(12));
  }

  @Test
  void includesPreviewColorOnEmptyCell() {
    givenPhotos();
    givenColorCells(colorCell(dongB, TimeBand.NIGHT, "#861318"));

    MyMapResponse response = myMapService.getMyMap(USER_ID, SeasonType.AUTUMN, "3105");

    MyMapResponse.Cell cell = cellOf(response, "B", TimeBand.NIGHT);

    assertThat(cell.isFilled()).isFalse();
    assertThat(cell.getPreviewColor()).isEqualTo("#861318");
    assertThat(cellOf(response, "A", TimeBand.DAY).getPreviewColor()).isNull();
  }

  private void givenPhotos(Photo... photos) {
    given(photoRepository.findMyMapPhotos(USER_ID, season)).willReturn(List.of(photos));
  }

  private void givenColorCells(ColorCell... cells) {
    given(colorCellRepository.findBySeason(season)).willReturn(List.of(cells));
  }

  private MyMapResponse.Cell cellOf(MyMapResponse response, String dongCode, TimeBand timeBand) {
    return response.getCells()
        .stream()
        .filter(cell -> cell.getDongCode().equals(dongCode) && cell.getTimeBand() == timeBand)
        .findFirst()
        .orElseThrow();
  }

  private Dong dong(String dongCode) {
    Dong dong = mock(Dong.class);
    given(dong.getDongCode()).willReturn(dongCode);
    given(dong.getName()).willReturn(dongCode + "동");

    return dong;
  }

  private Photo photo(Dong dong, TimeBand timeBand, List<Double> bins) {
    Photo photo = mock(Photo.class);
    given(photo.getDong()).willReturn(dong);
    given(photo.getTimeBand()).willReturn(timeBand);
    given(photo.getBins()).willReturn(bins);
    given(photo.getWeight()).willReturn(1.0f);

    return photo;
  }

  private ColorCell colorCell(Dong dong, TimeBand timeBand, String dominantColor) {
    ColorCell cell = mock(ColorCell.class);
    given(cell.getDong()).willReturn(dong);
    given(cell.getTimeBand()).willReturn(timeBand);
    given(cell.getDominantColor()).willReturn(dominantColor);

    return cell;
  }

  private List<Double> bins(Map<Integer, Double> values) {
    List<Double> bins = new ArrayList<>(Collections.nCopies(40, 0.0));
    values.forEach(bins::set);

    return bins;
  }
}
