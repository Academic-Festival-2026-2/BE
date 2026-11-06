package com.pixel.domain.colorcell;

import static org.assertj.core.api.Assertions.assertThat;

import com.pixel.domain.colorcell.dto.ColorRatio;
import com.pixel.domain.colorcell.entity.ColorCell;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.util.ColorBins;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ColorBinsTest {

  @Test
  void emptyWhenNoPhoto() {
    List<Double> bins = new ArrayList<>(Collections.nCopies(40, 0.0));

    assertThat(ColorBins.topRatios(bins, 5)).isEmpty();
  }

  @Test
  void sortsByLargestBinAsRatio() {
    List<Double> bins = new ArrayList<>(Collections.nCopies(40, 0.0));
    bins.set(7, 0.85);
    bins.set(36, 0.30);
    bins.set(27, 0.25);
    bins.set(8, 0.60);

    List<ColorRatio> result = ColorBins.topRatios(bins, 5);

    assertThat(result).hasSize(4);
    assertThat(result.get(0)).isEqualTo(new ColorRatio("#E37B1C", 0.43));
    assertThat(result.get(3).hex()).isEqualTo("#131886");
  }
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"0.15,0", "0.16,0", "0.14,36"})
  void dominantThreshold(double chromatic, int expected) {
    var bins = new ArrayList<>(Collections.nCopies(40, 0.0));
    bins.set(0, chromatic);
    bins.set(36, 1 - chromatic);
    assertThat(ColorBins.dominantColor(bins)).isEqualTo(ColorBins.HEX.get(expected));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {0, 36})
  void tiedMaximumUsesSmallestIndex(int start) {
    var bins = new ArrayList<>(Collections.nCopies(40, 0.0));
    bins.set(start, 0.5);
    bins.set(start + 1, 0.5);
    assertThat(ColorBins.dominantColor(bins)).isEqualTo(ColorBins.HEX.get(start));
  }

  @Test
  void zeroHasNoDominantColor() {
    assertThat(ColorBins.dominantColor(Collections.nCopies(40, 0.0))).isNull();
  }

  @Test
  void rejectsInvalidDistribution() {
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
        () -> ColorBins.dominantColor(null));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
        () -> ColorBins.dominantColor(Collections.nCopies(39, 0.0)));
    for (Double value : java.util.Arrays.asList(null, -1.0, Double.NaN,
        Double.POSITIVE_INFINITY, Double.MAX_VALUE)) {
      org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
          () -> ColorBins.dominantColor(Collections.nCopies(40, value)));
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(doubles = {1e-100, 1.0, 1e100})
  void repeatedBoundaryDistributionStaysChromatic(double scale) {
    var bins = new ArrayList<>(Collections.nCopies(40, 0.0));
    for (int i = 0; i < 21; i++) {
      bins.set(0, bins.get(0) + 0.15 * scale);
      bins.set(36, bins.get(36) + 0.85 * scale);
    }
    assertThat(ColorBins.dominantColor(bins)).isEqualTo(ColorBins.HEX.get(0));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(doubles = {1e-100, 1.0, 1e100})
  void genuinelyBelowBoundaryStaysAchromatic(double scale) {
    var bins = new ArrayList<>(Collections.nCopies(40, 0.0));
    bins.set(0, (0.15 - 1e-10) * scale);
    bins.set(36, (0.85 + 1e-10) * scale);
    assertThat(ColorBins.dominantColor(bins)).isEqualTo(ColorBins.HEX.get(36));
  }

  @Test
  void repeatedContributionsKeepBoundaryColor() {
    var cell = new ColorCell(null, null, TimeBand.DAY);
    var bins = new ArrayList<>(Collections.nCopies(40, 0.0));
    bins.set(0, 0.15);
    bins.set(36, 0.85);
    for (long id = 1; id <= 21; id++) {
      cell.contribute(id, bins, 1.0f, id == 1);
    }
    assertThat(cell.getDominantColor()).isEqualTo(ColorBins.HEX.get(0));
    assertThat(cell.getPhotoCount()).isEqualTo(21);
    assertThat(cell.getShotCount()).isEqualTo(1);
    assertThat(cell.getFirstPhotoId()).isEqualTo(1L);
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
        () -> cell.contribute(22L, Collections.nCopies(40, -1.0), 1.0f, false));
    assertThat(cell.getPhotoCount()).isEqualTo(21);
  }

  @Test
  void excludesColorRoundedToZero() {
    List<Double> bins = new ArrayList<>(Collections.nCopies(40, 0.0));
    bins.set(0, 0.96);
    bins.set(12, 0.004);

    List<ColorRatio> result = ColorBins.topRatios(bins, 5);

    assertThat(result)
        .extracting(ColorRatio::hex)
        .containsExactly(ColorBins.HEX.get(0));

    assertThat(result)
        .extracting(ColorRatio::ratio)
        .containsExactly(1.0);
  }
}
