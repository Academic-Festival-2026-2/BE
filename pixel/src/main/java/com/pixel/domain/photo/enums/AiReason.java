package com.pixel.domain.photo.enums;

// PERSONAL_ONLY 판정 사유(업로드 응답용). FE가 안내 문구를 고르는 데 쓴다.
public enum AiReason {
  // 음식·셀카·실내 등 동네 풍경이 주인공이 아닌 사진
  NOT_SCENERY,
  // 얼굴·차량 번호판이 알아볼 수 있을 만큼 크게 나온 사진
  PRIVACY
}
