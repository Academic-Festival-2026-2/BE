package com.binteum.domain.photo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PhotoUploadRequest(
    String dongCode,
    Double latitude,
    Double longitude,
    @JsonFormat(pattern = "uuuu-MM-dd'T'HH:mm:ss", lenient = OptBoolean.FALSE)
    LocalDateTime takenAt,
    LocalDate shotDate,
    Boolean isPublic,
    List<String> palette,
    List<Double> bins) {}
