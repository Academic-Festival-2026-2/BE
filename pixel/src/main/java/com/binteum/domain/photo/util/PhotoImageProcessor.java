package com.binteum.domain.photo.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
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
      BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
      if (source == null) {
        throw new IllegalArgumentException("JPEG 또는 PNG 이미지만 업로드할 수 있습니다.");
      }
      var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(original));
      var exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
      Integer orientation = exif == null ? null : exif.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
      boolean png = original.length >= 4 && original[0] == (byte) 0x89
          && original[1] == 0x50 && original[2] == 0x4e && original[3] == 0x47;
      // A fresh raster carries no source EXIF, GPS, text, or device metadata.
      BufferedImage image = orient(source, orientation == null ? 1 : orientation, png);
      double scale = Math.min(1.0, 600.0 / Math.max(image.getWidth(), image.getHeight()));
      int width = Math.max(1, (int) Math.round(image.getWidth() * scale));
      int height = Math.max(1, (int) Math.round(image.getHeight() * scale));
      BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = thumbnail.createGraphics();
      try {
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(image, 0, 0, width, height, null);
      } finally {
        graphics.dispose();
      }
      return new ImageData(encode(image, png ? "png" : "jpg"), encode(thumbnail, "jpg"));
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("이미지를 처리할 수 없습니다.", e);
    }
  }

  private BufferedImage orient(BufferedImage source, int orientation, boolean png) {
    int width = source.getWidth();
    int height = source.getHeight();
    boolean swap = orientation >= 5 && orientation <= 8;
    BufferedImage target = new BufferedImage(swap ? height : width, swap ? width : height,
        png ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int dx = x;
        int dy = y;
        switch (orientation) {
          case 2 -> dx = width - 1 - x;
          case 3 -> { dx = width - 1 - x; dy = height - 1 - y; }
          case 4 -> dy = height - 1 - y;
          case 5 -> { dx = y; dy = x; }
          case 6 -> { dx = height - 1 - y; dy = x; }
          case 7 -> { dx = height - 1 - y; dy = width - 1 - x; }
          case 8 -> { dx = y; dy = width - 1 - x; }
          default -> { }
        }
        target.setRGB(dx, dy, source.getRGB(x, y));
      }
    }
    return target;
  }

  private byte[] encode(BufferedImage image, String format) throws java.io.IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    if (!ImageIO.write(image, format, output)) {
      throw new IllegalArgumentException("이미지 인코딩에 실패했습니다.");
    }
    return output.toByteArray();
  }

  public record ImageData(byte[] originalBytes, byte[] thumbnailBytes) {}
}
