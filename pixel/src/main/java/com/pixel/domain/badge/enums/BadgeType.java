package com.pixel.domain.badge.enums;

// 배지 종류. 선언 순서가 배지 목록 화면의 순서다.
// 바꾸면 user_badges의 ck_user_badges_type 제약과 FE 배지 이름·그림도 같이 바꾼다.
public enum BadgeType {
  // 첫 사진을 올림
  FIRST_PHOTO,
  // 모두의 지도에서 그 동·계절(찍은 날짜 기준)의 첫 사진
  DONG_FIRST,
  // 모두의 지도에서 그 동·계절(찍은 날짜 기준)의 처음 10장 안
  DONG_TOP10,
  // 서로 다른 동 5곳에 사진을 올림
  DONG_FIVE,
  // 새벽·낮·노을·밤 네 시간대 사진을 모두 올림
  ALL_TIME_BANDS,
  // 사진 10장을 올림
  PHOTO_TEN
}
