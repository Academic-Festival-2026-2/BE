package com.pixel.domain.user.enums;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public enum ProfileImage {
  COLOR_ORANGE, COLOR_SKY, COLOR_NAVY, COLOR_GRAY,
  BUNNY_GRAY, BUNNY_MOCHA, BUNNY_STRAWBERRY, BUNNY_WHITE,
  CAT_BLUEGRAY, CAT_CHEESE, CAT_CHOCO, CAT_WHITE,
  DINO_LAVENDER, DINO_MINT, DINO_PINK, DINO_SKY,
  DOG_CREAM, DOG_GRAY, DOG_SHIBA, DOG_WHITE;

  private static final ProfileImage[] CHARACTERS =
      Arrays.stream(values()).filter(v -> !v.name().startsWith("COLOR_"))
          .toArray(ProfileImage[]::new);

  public static ProfileImage random() {
    return CHARACTERS[ThreadLocalRandom.current().nextInt(CHARACTERS.length)];
  }
}
