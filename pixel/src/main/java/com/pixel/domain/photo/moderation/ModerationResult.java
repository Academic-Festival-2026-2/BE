package com.pixel.domain.photo.moderation;

import com.pixel.domain.photo.enums.AiVerdict;

// 사진 검수 결과. REJECT만 저장하지 않고, 나머지는 판정 값으로 저장한다.
public record ModerationResult(Decision decision, Reason reason) {

  public enum Decision {
    PUBLIC,
    PERSONAL_ONLY,
    UNCHECKED,
    REJECT
  }

  // 판정 사유. PUBLIC·UNCHECKED는 사유가 없다(null).
  public enum Reason {
    NOT_SCENERY,
    PRIVACY,
    INAPPROPRIATE
  }

  public static ModerationResult approved() {
    return new ModerationResult(Decision.PUBLIC, null);
  }

  public static ModerationResult personalOnly(Reason reason) {
    return new ModerationResult(Decision.PERSONAL_ONLY, reason);
  }

  public static ModerationResult unchecked() {
    return new ModerationResult(Decision.UNCHECKED, null);
  }

  public static ModerationResult rejected() {
    return new ModerationResult(Decision.REJECT, Reason.INAPPROPRIATE);
  }

  public boolean isRejected() {
    return decision == Decision.REJECT;
  }

  public AiVerdict toAiVerdict() {
    return switch (decision) {
      case PUBLIC -> AiVerdict.PUBLIC;
      case PERSONAL_ONLY -> AiVerdict.PERSONAL_ONLY;
      case UNCHECKED -> AiVerdict.UNCHECKED;
      case REJECT -> throw new IllegalStateException("거절된 사진은 저장하지 않습니다.");
    };
  }
}
