package com.pixel.domain.map.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.pixel.domain.colorcell.entity.ColorCell;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.colorcell.util.ColorBins;
import com.pixel.domain.colorcell.dto.ColorRatio;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.dong.repository.DongRepository;
import com.pixel.domain.map.dto.MapDongsResponse;
import com.pixel.domain.season.entity.Season;
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
class MapServiceImplTest {

  @Mock
  private DongRepository dongRepository;

  @Mock
  private SeasonService seasonService;

  @Mock
  private ColorCellRepository colorCellRepository;

  @InjectMocks
  private MapServiceImpl mapService;

  private final Season season = mock(Season.class);

  @BeforeEach
  void setUp() {
    given(seasonService.getCurrentSeason()).willReturn(season);
  }

  @Test
  void excludesDongWithoutPhoto() {
    givenCells(cell("A", 5), cell("B", 0), cell("C", 3));

    MapDongsResponse response = mapService.getDongs(TimeBand.DAY);

    assertThat(response.getTopDongs())
        .extracting(MapDongsResponse.TopDong::getDongCode)
        .containsExactly("A", "C");
  }

  @Test
  void returnsTop3ByPhotoCountDesc() {
    givenCells(cell("A", 1), cell("B", 4), cell("C", 2), cell("D", 3));

    MapDongsResponse response = mapService.getDongs(TimeBand.DAY);

    assertThat(response.getTopDongs())
        .extracting(MapDongsResponse.TopDong::getDongCode)
        .containsExactly("B", "D", "C");
  }

  @Test
  void sortsTiesByDongCode() {
    givenCells(cell("B", 2), cell("A", 2), cell("C", 9));

    MapDongsResponse response = mapService.getDongs(TimeBand.DAY);

    assertThat(response.getTopDongs())
        .extracting(MapDongsResponse.TopDong::getDongCode)
        .containsExactly("C", "A", "B");
  }

  @Test
  void emptyWhenNoPhoto() {
    givenCells(cell("A", 0), cell("B", 0));

    MapDongsResponse response = mapService.getDongs(TimeBand.DAY);

    assertThat(response.getTopDongs()).isEmpty();
  }

  @Test
  void includesColorRatioForEachDong() {
    givenCells(cellWithBins("A", 5, bins(Map.of(0, 0.6, 12, 0.4)))
        , cell("B", 0));

    MapDongsResponse response = mapService.getDongs(TimeBand.DAY);

    MapDongsResponse.DongColor dongA =response.getDongs().get(0);
    assertThat(dongA.getColorRatio())
        .extracting(ColorRatio::hex)
        .containsExactly(ColorBins.HEX.get(0),ColorBins.HEX.get(12));

    assertThat(dongA.getColorRatio())
        .extracting(ColorRatio::ratio)
            .containsExactly(0.6, 0.4);

    assertThat(response.getDongs().get(1).getColorRatio()).isEmpty();

    assertThat(response.getTopDongs().get(0).getColorRatio())
        .extracting(ColorRatio::hex)
        .containsExactly(ColorBins.HEX.get(0),ColorBins.HEX.get(12));
  }

  private void givenCells(ColorCell... cells) {
    given(colorCellRepository.findForMap(season, TimeBand.DAY))
        .willReturn(List.of(cells));
  }

  private ColorCell cell(String dongCode, int photoCount) {
    Dong dong = mock(Dong.class);
    given(dong.getDongCode()).willReturn(dongCode);

    ColorCell cell = mock(ColorCell.class);
    given(cell.getDong()).willReturn(dong);
    given(cell.getPhotoCount()).willReturn(photoCount);
    return cell;
  }

  private ColorCell cellWithBins(String dongCode, int photoCount, List<Double> binsSum) {
    ColorCell cell = cell(dongCode, photoCount);
    given(cell.getBinsSum()).willReturn(binsSum);
    return cell;
  }

  private List<Double> bins(Map<Integer, Double> values) {
    List<Double> bins = new ArrayList<>(Collections.nCopies(40, 0.0));
    values.forEach(bins::set);
    return bins;
  }
}
