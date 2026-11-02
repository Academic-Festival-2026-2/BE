package com.binteum.domain.photo.dto;

import com.binteum.domain.season.enums.SeasonType;

public record PhotoUploadResponse(
    Long photoId,
    String dongCode,
    String name,
    SeasonType season,
    String timeBand,
    boolean exifVerified,
    boolean isPublic) {}
