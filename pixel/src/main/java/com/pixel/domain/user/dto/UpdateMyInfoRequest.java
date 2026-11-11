package com.pixel.domain.user.dto;

import com.pixel.domain.user.enums.ProfileImage;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateMyInfoRequest {

  private static final java.util.regex.Pattern EDGE_SPACES =
      java.util.regex.Pattern.compile("^[\\s\\p{Z}]+|[\\s\\p{Z}]+$");

  @Size(min = 2, max = 16, message = "닉네임은 2~16자여야 합니다.")
  @Pattern(regexp = "(?s)^(?!탈퇴회원).*$", message = "사용할 수 없는 닉네임입니다.")
  private String nickname;

  private Boolean locationAgreed;

  // 목록에 없는 값이면 JSON 변환 단계에서 400
  private ProfileImage profileImage;

  public void setNickname(String nickname) {
    this.nickname = nickname == null ? null : EDGE_SPACES.matcher(nickname).replaceAll("");
  }
}
