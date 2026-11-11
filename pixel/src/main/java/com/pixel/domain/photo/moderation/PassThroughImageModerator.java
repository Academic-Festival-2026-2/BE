package com.pixel.domain.photo.moderation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 검수를 끈 상태(로컬·테스트·시드 등록, 시연 중 API 장애)에서는 외부 API를 부르지 않고 모두 통과시킨다.
@Component
@ConditionalOnProperty(name = "moderation.enabled", havingValue = "false", matchIfMissing = true)
public class PassThroughImageModerator implements ImageModerator {

  @Override
  public ModerationResult moderate(byte[] jpegThumbnail) {
    return ModerationResult.approved();
  }
}
