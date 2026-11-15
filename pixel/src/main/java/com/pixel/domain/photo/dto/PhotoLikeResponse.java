package com.pixel.domain.photo.dto;

public record PhotoLikeResponse(Long photoId, boolean liked, long likeCount) {}
