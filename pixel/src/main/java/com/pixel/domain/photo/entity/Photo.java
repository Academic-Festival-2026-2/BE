package com.pixel.domain.photo.entity;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.photo.enums.AiVerdict;
import com.pixel.domain.photo.exception.code.PhotoErrorCode;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.user.entity.User;
import com.pixel.global.apiPayload.exception.GeneralException;
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

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "ai_verdict", length = 13, nullable = false)
  private AiVerdict aiVerdict;

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

  // 풍경이 아니거나 얼굴·번호판이 크게 나온 사진, 검수하지 못한 사진은 공개로 바꿀 수 없다.
  // 공개 가능 여부는 이 메서드에서만 검사한다.
  public void changePublic(boolean isPublic) {
    if (isPublic && !canBePublic()) {
      throw new GeneralException(PhotoErrorCode.NOT_PUBLISHABLE);
    }
    this.isPublic = isPublic;
  }

  public boolean countsForPublicMap() {
    return countsForPublicMap(exifVerified, aiVerdict);
  }

  // 모두의 지도(칸 색 집계, 공개 사진 점)에 들어가는 사진인지 판단한다.
  // 업로드 시 동 잠금, 집계(aggregate), 차감(retract)이 모두 이 기준을 써야 칸 색이 어긋나지 않는다.
  public static boolean countsForPublicMap(boolean exifVerified, AiVerdict aiVerdict) {
    return exifVerified && aiVerdict == AiVerdict.PUBLIC;
  }

  public boolean canBePublic() {
    return aiVerdict == AiVerdict.PUBLIC;
  }

  public Photo(
      User user,
      Dong dong,
      Season season,
      TimeBand timeBand,
      LocalDateTime takenAt,
      LocalDate shotDate,
      boolean exifVerified,
      AiVerdict aiVerdict,
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
    this.aiVerdict = aiVerdict;
    this.palette = palette;
    this.bins = bins;
    this.imageKey = imageKey;
    this.thumbnailKey = thumbnailKey;
    this.imageHash = imageHash;
    // 검수를 통과하지 않은 사진은 공개 요청이 와도 비공개로 저장한다.
    this.isPublic = isPublic && aiVerdict == AiVerdict.PUBLIC;
    this.nearbyOnly = false;
  }
}
