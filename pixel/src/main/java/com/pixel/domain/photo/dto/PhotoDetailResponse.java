package com.pixel.domain.photo.dto;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.season.enums.SeasonType;
import java.time.LocalDateTime;
import java.util.List;

// liked는 로그인한 회원이 이 사진에 좋아요를 눌렀는지. 비로그인이면 false
public record PhotoDetailResponse(Long photoId, String imageUrl, String nickname, String name,
    LocalDateTime takenAt, TimeBand timeBand, List<String> palette, List<Double> bins,
    SeasonType season, long likeCount, boolean liked) {}
