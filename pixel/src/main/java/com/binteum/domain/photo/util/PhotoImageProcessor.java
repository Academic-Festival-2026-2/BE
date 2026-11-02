package com.binteum.domain.photo.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class PhotoImageProcessor {

  private static final float JPEG_QUALITY = 0.90f;

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
      // 입력 파일의 메타데이터를 전달하지 않고 픽셀만 다시 인코딩한다.
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
    if (orientation < 2 || orientation > 8) {
      return source;
    }
    int width = source.getWidth();
    int height = source.getHeight();
    boolean swap = orientation >= 5;
    BufferedImage target = new BufferedImage(swap ? height : width, swap ? width : height,
        png ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
    AffineTransform transform = switch (orientation) {
      case 2 -> new AffineTransform(-1, 0, 0, 1, width, 0);
      case 3 -> new AffineTransform(-1, 0, 0, -1, width, height);
      case 4 -> new AffineTransform(1, 0, 0, -1, 0, height);
      case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
      case 6 -> new AffineTransform(0, 1, -1, 0, height, 0);
      case 7 -> new AffineTransform(0, -1, -1, 0, height, width);
      case 8 -> new AffineTransform(0, -1, 1, 0, 0, width);
      default -> throw new IllegalArgumentException("올바르지 않은 이미지 방향입니다.");
    };
    Graphics2D graphics = target.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      graphics.drawImage(source, transform, null);
    } finally {
      graphics.dispose();
    }
    return target;
  }

  private byte[] encode(BufferedImage image, String format) throws java.io.IOException {
    // 사용자 ICC 프로필을 재첨부하지 않도록 기존 출력과 동일한 sRGB로 변환한다.
    if (!image.getColorModel().getColorSpace().isCS_sRGB()) {
      BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(),
          image.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = rgb.createGraphics();
      try {
        graphics.drawImage(image, 0, 0, null);
      } finally {
        graphics.dispose();
      }
      image = rgb;
    }
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    if (format.equals("jpg")) {
      var writers = ImageIO.getImageWritersByFormatName("jpeg");
      if (!writers.hasNext()) {
        throw new IllegalArgumentException("JPEG 인코더를 찾을 수 없습니다.");
      }
      var writer = writers.next();
      try (var stream = ImageIO.createImageOutputStream(output)) {
        writer.setOutput(stream);
        var parameters = writer.getDefaultWriteParam();
        parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parameters.setCompressionQuality(JPEG_QUALITY);
        writer.write(null, new IIOImage(image, null, null), parameters);
      } finally {
        writer.dispose();
      }
    } else if (!ImageIO.write(image, format, output)) {
      throw new IllegalArgumentException("이미지 인코딩에 실패했습니다.");
    }
    return output.toByteArray();
  }

  public record ImageData(byte[] originalBytes, byte[] thumbnailBytes) {}
}
