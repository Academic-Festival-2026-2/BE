package com.pixel.domain.photo.dto;

import com.pixel.domain.colorcell.enums.TimeBand;
import java.time.LocalDateTime;
import java.util.List;

// imageUrl은 원본, displayUrl은 크게 보기용 축소 이미지(긴 변 2048px 이하 JPEG).
// 축소 이미지가 없는 사진(이 기능 전에 올린 사진)은 displayUrl이 null → 원본을 쓴다.
// liked는 로그인한 회원이 이 사진에 좋아요를 눌렀는지. 비로그인이면 false
public record PhotoDetailResponse(Long photoId, String imageUrl, String displayUrl,
    String nickname, String name, LocalDateTime takenAt, TimeBand timeBand,
    List<String> palette, List<Double> bins, long likeCount, boolean liked) {}
