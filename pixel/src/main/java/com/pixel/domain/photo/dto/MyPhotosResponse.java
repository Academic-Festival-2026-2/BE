package com.pixel.domain.photo.dto;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.photo.enums.AiVerdict;
import java.time.LocalDateTime;
import java.util.List;

public record MyPhotosResponse(List<Item> photos, int page, int size, long totalElements,
    boolean hasNext) {

  public record Item(Long photoId, String thumbnailUrl, String dongCode, String name,
      TimeBand timeBand, LocalDateTime takenAt, String color, boolean isPublic,
      AiVerdict aiVerdict) {}
}
