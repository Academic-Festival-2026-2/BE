package com.pixel.domain.photo.dto;

import com.pixel.domain.badge.enums.BadgeType;
import com.pixel.domain.photo.enums.AiReason;
import com.pixel.domain.photo.enums.AiVerdict;
import com.pixel.domain.season.enums.SeasonType;
import java.util.List;

public record PhotoUploadResponse(
    Long photoId,
    String dongCode,
    String name,
    SeasonType season,
    String timeBand,
    boolean exifVerified,
    boolean isPublic,
    AiVerdict aiVerdict,
    // PERSONAL_ONLY일 때 NOT_SCENERY(풍경 아님) 또는 PRIVACY(얼굴·번호판). 그 외에는 null
    AiReason aiReason,
    Integer cellPhotoCount,
    boolean firstInCell,
    // 이번 업로드로 새로 받은 배지 (없으면 빈 배열)
    List<BadgeType> newBadges) {}
