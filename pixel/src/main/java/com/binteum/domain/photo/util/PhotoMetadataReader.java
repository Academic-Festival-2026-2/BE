package com.binteum.domain.photo.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import org.springframework.stereotype.Component;

@Component
public class PhotoMetadataReader {

  private static final DateTimeFormatter EXIF_FORMAT =
      DateTimeFormatter.ofPattern("uuuu:MM:dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);

  public PhotoMetadata read(byte[] bytes) {
    LocalDateTime takenAt = null;
    Double latitude = null;
    Double longitude = null;

    try {
      Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));

      ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

      if (exif != null) {
        String dateText = exif.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);

        if (dateText != null) {
          try {
            takenAt = LocalDateTime.parse(dateText.trim(), EXIF_FORMAT);
          } catch (RuntimeException ignored) {
            // 읽을 수 없는 촬영 시각은 누락으로 처리
          }
        }
      }

      GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);

      if (gps != null) {
        GeoLocation location = gps.getGeoLocation();

        if (location != null && !location.isZero()) {
          latitude = location.getLatitude();
          longitude = location.getLongitude();
        }
      }

    } catch (Exception ignored) {
      // EXIF가 없거나 읽지 못해도 수동 입력 허용
    }

    return new PhotoMetadata(takenAt, latitude, longitude);
  }

  public record PhotoMetadata(LocalDateTime takenAt, Double latitude, Double longitude) {}
}
