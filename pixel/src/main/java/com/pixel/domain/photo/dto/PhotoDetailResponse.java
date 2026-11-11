package com.pixel.domain.photo.dto;

import com.pixel.domain.colorcell.enums.TimeBand;
import java.time.LocalDateTime;
import java.util.List;

public record PhotoDetailResponse(Long photoId, String imageUrl, String nickname, String name,
                                  LocalDateTime takenAt, TimeBand timeBand, List<String> palette,
                                  List<Double> bins) {

}
