package com.pixel.domain.colorcell.util;

import com.pixel.domain.colorcell.dto.ColorRatio;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

public final class ColorBins {

  public static final List<String> HEX = List.of(
      "#D4E6F5", "#9CC8EC", "#5A8FD0", "#A9B6CC", "#B2A6D6", "#F2AEB8", "#F39A52",
      "#F7BD6E", "#5B5592", "#1E2850", "#3C8FB5", "#4FA7A3", "#2A5E78", "#5C9A55", "#36603D",
      "#A9CF72", "#C2AE72", "#EDD23A", "#E2783A", "#C9432F", "#F6C6D3", "#E27BA6", "#9B7BC8",
      "#EAE2D2", "#D9C7A6", "#A9573F", "#8A6245", "#A8DCCF", "#F8DD98", "#DA5A95", "#4F63D6",
      "#43BFC4", "#E8384F", "#7A3FA0", "#23262D", "#3E434E", "#6C7078", "#A2A5AA", "#C9CDD3",
      "#F0F2F4"
  );

  /** 0 ~ CHROMATIC_COUNT-1은 유채색, 그 뒤 6개(먹색 → 구름)는 무채색이다. */
  public static final int CHROMATIC_COUNT = 34;

  private ColorBins() {
  }

  /** 색상 분포의 길이, 각 값과 합계가 유효한지 검사한다. */
  public static void validateBins(List<Double> bins) {
    if (bins == null || bins.size() != HEX.size()
        || bins.stream().anyMatch(v -> v == null || !Double.isFinite(v) || v < 0)) {
      throw new IllegalArgumentException("색상 분포는 40개의 유효한 음이 아닌 숫자여야 합니다.");
    }
    double total = bins.stream().mapToDouble(Double::doubleValue).sum();
    if (!Double.isFinite(total)) {
      throw new IllegalArgumentException("색상 분포 합계가 유효하지 않습니다.");
    }
  }

  /**
   * 유채색 비율이 15% 이상이면 0~CHROMATIC_COUNT-1, 미만이면 무채색 CHROMATIC_COUNT~39 중 최댓값을 선택한다.
   * 동점이면 작은 인덱스를 선택하며, 전체 합계가 0이면 null을 반환한다.
   */
  public static String dominantColor(List<Double> bins) {
    validateBins(bins);
    double total = bins.stream().mapToDouble(Double::doubleValue).sum();
    if (total == 0) {
      return null;
    }
    double chromatic = bins.subList(0, CHROMATIC_COUNT)
        .stream().mapToDouble(Double::doubleValue).sum();
    double ratio = chromatic / total;
    double threshold = 0.15;
    // 비율 크기에 비례하는 극소 오차만 허용해 반복 누적 시 15% 경계가 뒤집히지 않게 한다.
    double tolerance = Math.max(Math.abs(ratio), threshold) * 1e-12;
    int start = ratio >= threshold || threshold - ratio <= tolerance ? 0 : CHROMATIC_COUNT;
    int end = start == 0 ? CHROMATIC_COUNT : HEX.size();
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
        .filter(color -> color.ratio() > 0)
        .toList();
  }
}
