package com.pixel.domain.colorcell.util;

import com.pixel.domain.colorcell.dto.ColorRatio;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public final class ColorBins {

  public static final List<String> HEX = List.of(
      "#861318", "#E31C24", "#F17E83",
      "#863213", "#E3511C", "#F19D7E",
      "#864A13", "#E37B1C", "#F1B57E",
      "#866413", "#E3A91C", "#F1CF7E",
      "#868113", "#E3DB1C", "#F1EC7E",
      "#608613", "#A1E31C", "#CBF17E",
      "#13861D", "#1CE32D", "#7EF188",
      "#13867C", "#1CE3D2", "#7EF1E7",
      "#135186", "#1C88E3", "#7EBCF1",
      "#131886", "#1C24E3", "#7E83F1",
      "#561386", "#901CE3", "#C17EF1",
      "#861360", "#E31CA1", "#F17ECB",
      "#1C1C1C", "#555555", "#9E9E9E", "#E6E6E6"
  );

  private ColorBins() {
  }

  public static String dominantColor(List<Double> bins) {
    if (bins == null || bins.size() != HEX.size()
        || bins.stream().anyMatch(v -> v == null || !Double.isFinite(v) || v < 0)) {
      throw new IllegalArgumentException("색상 분포는 40개의 유효한 음이 아닌 숫자여야 합니다.");
    }
    double total = bins.stream().mapToDouble(Double::doubleValue).sum();
    if (!Double.isFinite(total)) {
      throw new IllegalArgumentException("색상 분포 합계가 유효하지 않습니다.");
    }
    if (total == 0) {
      return null;
    }
    double chromatic = bins.subList(0, 36).stream().mapToDouble(Double::doubleValue).sum();
    int start = chromatic / total >= 0.15 ? 0 : 36;
    int end = start == 0 ? 36 : 40;
    int best = start;
    for (int i = start + 1; i < end; i++) {
      if (bins.get(i) > bins.get(best)) {
        best = i;
      }
    }
    return HEX.get(best);
  }

  public static List<ColorRatio> topRatios(List<Double> bins, int limit) {
    double total = bins.stream().mapToDouble(Double::doubleValue).sum();
    if (total == 0) {
      return List.of();
    }
    return IntStream.range(0, bins.size())
        .boxed()
        .filter(i -> bins.get(i) > 0)
        .sorted(Comparator.comparingDouble((Integer i) -> bins.get(i)).reversed())
        .limit(limit)
        .map(i -> new ColorRatio(HEX.get(i),
            Math.round(bins.get(i) / total * 100) / 100.0))
        .toList();
  }
}
