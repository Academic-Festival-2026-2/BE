package com.pixel.domain.colorcell.entity;

import com.pixel.domain.colorcell.enums.CellStatus;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.util.ColorBins;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.season.entity.Season;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "color_cells", uniqueConstraints = @UniqueConstraint(
    columnNames = {"dong_code", "season_id", "time_band"}))
public class ColorCell {

  private static final int MIN_SHOTS_FOR_OK = 5;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "cell_id")
  private Long cellId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dong_code", columnDefinition = "CHAR(10)", nullable = false)
  private Dong dong;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "season_id", nullable = false)
  private Season season;

  @Enumerated(EnumType.STRING)
  @Column(name = "time_band", length = 6, nullable = false)
  private TimeBand timeBand;

  @Column(name = "photo_count", nullable = false)
  private int photoCount;

  @Column(name = "shot_count", nullable = false)
  private int shotCount;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "bins_sum", nullable = false)
  private List<Double> binsSum;

  @Column(name = "dominant_color", columnDefinition = "CHAR(7)")
  private String dominantColor;

  @Column(name = "first_photo_id")
  private Long firstPhotoId;

  public ColorCell(Dong dong, Season season, TimeBand timeBand) {
    this.dong = dong;
    this.season = season;
    this.timeBand = timeBand;
    this.binsSum = new ArrayList<>(Collections.nCopies(40, 0.0));
  }

  public void contribute(Long photoId, List<Double> bins, float weight, boolean newShot) {
    ColorBins.validateBins(bins);
    if (photoId == null || !Float.isFinite(weight) || weight < 0 || weight > 1) {
      throw new IllegalArgumentException("사진 ID 또는 가중치가 유효하지 않습니다.");
    }
    List<Double> accumulated = new ArrayList<>(binsSum);
    for (int i = 0; i < 40; i++) {
      accumulated.set(i, accumulated.get(i) + bins.get(i) * weight);
    }
    String color = ColorBins.dominantColor(accumulated);
    // 현재 업로드 가중치는 1이다. 전부 0인 집계는 DB의 비어 있지 않은 셀 제약과 충돌한다.
    if (color == null) {
      throw new IllegalArgumentException("집계된 색상 분포 합계는 0보다 커야 합니다.");
    }
    if (photoCount == 0) {
      firstPhotoId = photoId;
    }
    photoCount = Math.addExact(photoCount, 1);
    if (newShot) {
      shotCount = Math.addExact(shotCount, 1);
    }
    binsSum = accumulated;
    dominantColor = color;
  }

  public void retract(List<Double> bins, float weight, boolean lastShot, Long nextFirstPhotoId) {
    ColorBins.validateBins(bins);
    if (photoCount <= 0 || !Float.isFinite(weight) || weight < 0 || weight > 1) {
      throw new IllegalStateException("차감할 집계 또는 가중치가 유효하지 않습니다.");
    }
    int count = photoCount - 1;
    int shots = shotCount - (lastShot ? 1 : 0);
    if (shots < 0 || shots > count || (count > 0 && nextFirstPhotoId == null)) {
      throw new IllegalStateException("사진 집계 정합성을 확인해야 합니다.");
    }
    List<Double> remaining = new ArrayList<>(binsSum);
    for (int i = 0; i < 40; i++) {
      double before = remaining.get(i);
      double removed = bins.get(i) * weight;
      double difference = before - removed;
      double tolerance = Math.max(Math.abs(before), Math.abs(removed)) * 1e-12;
      if (difference < -tolerance) {
        throw new IllegalStateException("차감할 색상 분포가 기존 집계보다 큽니다.");
      }
      remaining.set(i, count == 0 ? 0.0 : Math.max(0.0, difference));
    }
    String color = ColorBins.dominantColor(remaining);
    if (count > 0 && color == null) {
      throw new IllegalStateException("남은 사진의 색상 분포가 비어 있습니다.");
    }
    photoCount = count;
    shotCount = shots;
    binsSum = remaining;
    dominantColor = color;
    firstPhotoId = count == 0 ? null : nextFirstPhotoId;
  }

  public CellStatus getStatus() {
    if (photoCount == 0) {
      return CellStatus.EMPTY;
    }
    if (shotCount < MIN_SHOTS_FOR_OK) {
      return CellStatus.INSUFFICIENT;
    }
    return CellStatus.OK;
  }
}
