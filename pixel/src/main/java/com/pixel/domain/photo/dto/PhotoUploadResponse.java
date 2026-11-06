package com.pixel.domain.photo.dto;

import com.pixel.domain.season.enums.SeasonType;

public record PhotoUploadResponse(
    Long photoId,
    String dongCode,
    String name,
    SeasonType season,
    String timeBand,
    boolean exifVerified,
    boolean isPublic,
    Integer cellPhotoCount,
    boolean firstInCell) {}
