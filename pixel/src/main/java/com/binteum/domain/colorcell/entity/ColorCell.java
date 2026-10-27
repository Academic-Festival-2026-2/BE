package com.binteum.domain.colorcell.entity;

import com.binteum.domain.colorcell.enums.TimeBand;
import com.binteum.domain.dong.entity.Dong;
import com.binteum.domain.season.entity.Season;
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
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "color_cells")
public class ColorCell {

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
}
