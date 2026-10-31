package com.binteum.domain.photo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PhotoUploadRequest(
    String dongCode,
    Double latitude,
    Double longitude,
    LocalDateTime takenAt,
    LocalDate shotDate,
    Boolean isPublic,
    List<String> palette,
    List<Double> bins) {}
