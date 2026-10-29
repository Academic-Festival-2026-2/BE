package com.binteum.domain.photo.dto;

public record PhotoUploadResponse(
    Long photoId, String dongCode, String timeBand, boolean exifVerified, boolean isPublic) {}
