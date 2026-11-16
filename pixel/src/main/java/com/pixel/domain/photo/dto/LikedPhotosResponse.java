package com.pixel.domain.photo.dto;

import com.pixel.domain.colorcell.enums.TimeBand;
import java.time.LocalDateTime;
import java.util.List;

public record LikedPhotosResponse(List<Item> photos, int page, int size, long totalElements,
    boolean hasNext) {

  // nickname은 사진을 올린 회원
  public record Item(Long photoId, String thumbnailUrl, String dongCode, String name,
      String nickname, TimeBand timeBand, LocalDateTime takenAt, String color) {}
}
