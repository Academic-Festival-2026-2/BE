package com.pixel.domain.photo.moderation;

// 업로드 사진이 모두의 지도에 들어가도 되는지 판정한다.
// 구현체는 외부 API 실패를 예외로 던지지 않고 UNCHECKED로 돌려준다. 검수 때문에 업로드가 막히면 안 된다.
public interface ImageModerator {

  ModerationResult moderate(byte[] jpegThumbnail);
}
