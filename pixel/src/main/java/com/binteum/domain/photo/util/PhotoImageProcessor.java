package com.binteum.domain.photo.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class PhotoImageProcessor {

  public ImageData process(byte[] original) {
    try {
      BufferedImage image = ImageIO.read(new ByteArrayInputStream(original));

      if (image == null) {
        throw new IllegalArgumentException("JPEG 또는 PNG 이미지만 업로드할 수 있습니다.");
      }

      int width = image.getWidth();
      int height = image.getHeight();

      if (width <= 0 || height <= 0) {
        throw new IllegalArgumentException("올바르지 않은 이미지입니다.");
      }

      double scale = Math.min(1.0, 600.0 / Math.max(width, height));

      int thumbnailWidth = Math.max(1, (int) Math.round(width * scale));
      int thumbnailHeight = Math.max(1, (int) Math.round(height * scale));

      BufferedImage thumbnail =
          new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);

      Graphics2D graphics = thumbnail.createGraphics();

      try {
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, thumbnailWidth, thumbnailHeight);

        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        graphics.drawImage(image, 0, 0, thumbnailWidth, thumbnailHeight, null);
      } finally {
        graphics.dispose();
      }

      ByteArrayOutputStream output = new ByteArrayOutputStream();

      if (!ImageIO.write(thumbnail, "jpg", output)) {
        throw new IllegalStateException("썸네일 생성에 실패했습니다.");
      }

      return new ImageData(output.toByteArray());

    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("이미지를 처리할 수 없습니다.", e);
    }
  }

  public record ImageData(byte[] thumbnailBytes) {}
}
