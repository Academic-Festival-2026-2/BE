package com.pixel.domain.photo.enums;

// 업로드 시 AI 검수 결과. 거절(REJECT)된 사진은 저장하지 않으므로 값에 없다.
public enum AiVerdict {
  // 동네 풍경 사진: EXIF가 검증되면 모두의 지도에 반영하고 공개할 수 있다.
  PUBLIC,
  // 풍경이 아니거나 얼굴·번호판이 크게 나온 사진: 나의 지도에만 쓰고 공개할 수 없다.
  PERSONAL_ONLY,
  // 타임아웃·API 오류로 판정을 받지 못한 사진: PERSONAL_ONLY와 같게 처리한다.
  UNCHECKED
}
