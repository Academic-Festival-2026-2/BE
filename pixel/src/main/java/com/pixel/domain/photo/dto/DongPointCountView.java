package com.pixel.domain.photo.dto;

// 지도 API용 동별 점 개수 (동 상세에 점으로 보이는 사진 수)
public interface DongPointCountView {

  String getDongCode();

  Long getPointCount();
}
