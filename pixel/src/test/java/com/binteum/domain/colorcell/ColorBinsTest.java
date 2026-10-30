package com.binteum.domain.colorcell;

import static org.assertj.core.api.Assertions.assertThat;

import com.binteum.domain.colorcell.dto.ColorRatio;
import com.binteum.domain.colorcell.util.ColorBins;
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
}
