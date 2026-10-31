package com.binteum.domain.photo.entity;

import com.binteum.domain.colorcell.enums.TimeBand;
import com.binteum.domain.dong.entity.Dong;
import com.binteum.domain.season.entity.Season;
import com.binteum.domain.user.entity.User;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "photos")
public class Photo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "photo_id")
  private Long photoId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dong_code", columnDefinition = "CHAR(10)", nullable = false)
  private Dong dong;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "season_id", nullable = false)
  private Season season;

  @Enumerated(EnumType.STRING)
  @Column(name = "time_band", length = 6, nullable = false)
  private TimeBand timeBand;

  @Column(name = "taken_at")
  private LocalDateTime takenAt;

  @Column(name = "shot_date", nullable = false)
  private LocalDate shotDate;

  // location은 MySQL POINT SRID 4326 컬럼이다.
  // 추후 네이티브 공간 SQL로 저장/조회한다.
  // 현재 엔티티에는 매핑하지 않는다.

  @Column(name = "exif_verified", nullable = false)
  private boolean exifVerified;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "palette", nullable = false)
  private List<String> palette;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "bins", nullable = false)
  private List<Double> bins;

  @Column(name = "weight", nullable = false)
  private float weight = 1.0f;

  @Column(name = "image_key", length = 255, nullable = false)
  private String imageKey;

  @Column(name = "thumbnail_key", length = 255, nullable = false)
  private String thumbnailKey;

  @Column(name = "image_hash", columnDefinition = "CHAR(64)", nullable = false, unique = true)
  private String imageHash;

  @Column(name = "is_public", nullable = false)
  private boolean isPublic;

  @Column(name = "nearby_only", nullable = false)
  private boolean nearbyOnly;

  @Column(name = "created_at", insertable = false, updatable = false)
  private LocalDateTime createdAt;

  public Photo(
      User user,
      Dong dong,
      Season season,
      TimeBand timeBand,
      LocalDateTime takenAt,
      LocalDate shotDate,
      boolean exifVerified,
      List<String> palette,
      List<Double> bins,
      String imageKey,
      String thumbnailKey,
      String imageHash,
      boolean isPublic) {
    this.user = user;
    this.dong = dong;
    this.season = season;
    this.timeBand = timeBand;
    this.takenAt = takenAt;
    this.shotDate = shotDate;
    this.exifVerified = exifVerified;
    this.palette = palette;
    this.bins = bins;
    this.imageKey = imageKey;
    this.thumbnailKey = thumbnailKey;
    this.imageHash = imageHash;
    this.isPublic = isPublic;
    this.nearbyOnly = false;
  }
}
