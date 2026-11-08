package com.pixel.domain.photo.service;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.service.ColorCellService;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.dong.repository.DongRepository;
import com.pixel.domain.photo.dto.PhotoUploadRequest;
import com.pixel.domain.photo.dto.PhotoUploadResponse;
import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.photo.enums.AiVerdict;
import com.pixel.domain.photo.exception.code.PhotoErrorCode;
import com.pixel.domain.photo.moderation.ImageModerator;
import com.pixel.domain.photo.moderation.ModerationResult;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.photo.util.PhotoImageProcessor;
import com.pixel.domain.photo.util.PhotoMetadataReader;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.repository.SeasonRepository;
import com.pixel.domain.user.entity.User;
import com.pixel.domain.user.repository.UserRepository;
import com.pixel.global.apiPayload.exception.GeneralException;
import com.pixel.global.apiPayload.status.ErrorStatus;
import com.pixel.global.storage.S3StorageService;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoService {

  private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
  private static final double GPS_EPSILON = 0.00001;
  private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-F]{6}$");

  private final PhotoRepository photoRepository;
  private final UserRepository userRepository;
  private final DongRepository dongRepository;
  private final SeasonRepository seasonRepository;
  private final PhotoMetadataReader metadataReader;
  private final PhotoImageProcessor imageProcessor;
  private final S3StorageService storageService;
  private final ColorCellService colorCellService;
  private final ImageModerator imageModerator;
  private final TransactionTemplate transactionTemplate;

  public PhotoUploadResponse upload(Long userId, MultipartFile file, PhotoUploadRequest request) {
    if (file == null || file.isEmpty()) {
      throw new GeneralException(PhotoErrorCode.FILE_REQUIRED);
    }
    // HTTP 멀티파트 처리를 거치지 않는 내부 호출에도 용량 제한을 적용한다.
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new GeneralException(ErrorStatus.COMMON413);
    }
    if (request == null) {
      throw new GeneralException(PhotoErrorCode.METADATA_REQUIRED);
    }

    validateColors(request);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new GeneralException(PhotoErrorCode.UNAUTHORIZED));

    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (Exception e) {
      throw new GeneralException(PhotoErrorCode.FILE_UNREADABLE);
    }

    String imageType = detectImageType(bytes);
    String imageHash = sha256(bytes);

    if (photoRepository.existsByImageHash(imageHash)) {
      throw new GeneralException(PhotoErrorCode.DUPLICATE_PHOTO);
    }

    var exif = metadataReader.read(bytes);

    // 유효한 EXIF 촬영 시각을 우선하고, 누락된 경우에만 요청의 KST 시각을 사용한다.
    LocalDateTime takenAt = exif.takenAt() != null ? exif.takenAt() : request.takenAt();

    if (takenAt == null) {
      throw new GeneralException(PhotoErrorCode.TAKEN_AT_REQUIRED);
    }

    LocalDate shotDate = request.shotDate() != null ? request.shotDate() : takenAt.toLocalDate();

    if (!shotDate.equals(takenAt.toLocalDate())) {
      throw new GeneralException(PhotoErrorCode.SHOT_DATE_MISMATCH);
    }

    if ((request.latitude() == null) != (request.longitude() == null)) {
      throw new GeneralException(PhotoErrorCode.GPS_PAIR_REQUIRED);
    }

    Double latitude = request.latitude() != null ? request.latitude() : exif.latitude();

    Double longitude = request.longitude() != null ? request.longitude() : exif.longitude();

    if ((latitude == null) != (longitude == null)) {
      throw new GeneralException(PhotoErrorCode.GPS_PAIR_REQUIRED);
    }

    String dongCode;
    if (latitude != null) {
      validateCoordinates(latitude, longitude);

      dongCode =
          dongRepository
              .findDongCodeByCoordinates(latitude, longitude)
              .orElseThrow(() -> new GeneralException(PhotoErrorCode.GPS_OUTSIDE_AREA));

      if (request.dongCode() != null
          && !request.dongCode().isBlank()
          && !request.dongCode().equals(dongCode)) {
        throw new GeneralException(PhotoErrorCode.DONG_GPS_MISMATCH);
      }
    } else {
      dongCode = request.dongCode();
      if (dongCode == null || dongCode.isBlank()) {
        throw new GeneralException(PhotoErrorCode.DONG_REQUIRED);
      }
    }

    Dong dong = dongRepository.findById(dongCode).orElseThrow(() -> new GeneralException(PhotoErrorCode.DONG_NOT_FOUND));

    Season season =
        seasonRepository
            .findCurrent(shotDate)
            .orElseThrow(() -> new GeneralException(PhotoErrorCode.SEASON_NOT_FOUND));

    TimeBand timeBand = resolveTimeBand(takenAt.toLocalTime());

    boolean exifVerified =
        exif.takenAt() != null
            && exif.latitude() != null
            && exif.longitude() != null
            && exif.takenAt().equals(takenAt)
            && nearlyEqual(exif.latitude(), latitude)
            && nearlyEqual(exif.longitude(), longitude)
            && shotDate.equals(exif.takenAt().toLocalDate());

    PhotoImageProcessor.ImageData processed;
    try {
      processed = imageProcessor.process(bytes);
    } catch (IllegalArgumentException e) {
      throw new GeneralException(PhotoErrorCode.INVALID_IMAGE);
    }

    // S3 업로드·동 잠금 전에 썸네일로 AI 검수를 받는다.
    // 거절된 사진은 저장하지 않고, 검수를 기다리는 동안 같은 동의 다른 업로드를 막지 않는다.
    ModerationResult moderation = imageModerator.moderate(processed.thumbnailBytes());
    if (moderation.decision() != ModerationResult.Decision.PUBLIC) {
      // 오판 조사용. 사진 내용은 남기지 않고 판정과 사유만 기록한다.
      log.info("사진 검수 결과 {} (사유: {}, userId: {}, dong: {})",
          moderation.decision(), moderation.reason(), userId, dongCode);
    }
    if (moderation.isRejected()) {
      throw new GeneralException(PhotoErrorCode.MODERATION_REJECTED);
    }
    AiVerdict aiVerdict = moderation.toAiVerdict();

    // 검수까지는 트랜잭션 밖에서 처리해 외부 API를 기다리는 동안 DB 커넥션을 쥐지 않는다.
    // S3 업로드부터 저장·집계까지만 하나의 트랜잭션으로 묶는다.
    return transactionTemplate.execute(status -> {
      String fileId = UUID.randomUUID().toString();
      String originalKey = "photos/original/" + fileId + "." + imageType;
      String thumbnailKey = "photos/thumbnail/" + fileId + ".jpg";

      List<String> uploadedKeys = new ArrayList<>();

      // 외부 저장소(S3) 업로드 중 실패하면 성공한 파일을 정리한다.
      try {
        storageService.upload(
            originalKey, processed.originalBytes(), imageType.equals("png") ? "image/png" : "image/jpeg");
        uploadedKeys.add(originalKey);

        storageService.upload(thumbnailKey, processed.thumbnailBytes(), "image/jpeg");
        uploadedKeys.add(thumbnailKey);
      } catch (RuntimeException e) {
        storageService.deleteAllQuietly(uploadedKeys);
        throw e;
      }

      // S3와 DB는 하나의 트랜잭션이 아니므로
      // DB 롤백 시 S3에 남은 파일을 제거한다.
      if (!TransactionSynchronizationManager.isSynchronizationActive()) {
        storageService.deleteAllQuietly(uploadedKeys);
        throw new IllegalStateException("DB 트랜잭션이 활성화되지 않았습니다.");
      }

      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              if (status != STATUS_COMMITTED) {
                storageService.deleteAllQuietly(uploadedKeys);
              }
            }
          });

      // 모두의 지도에 집계될 사진만 동 잠금을 잡는다. aggregate·retract와 같은 기준을 쓴다.
      if (Photo.countsForPublicMap(exifVerified, aiVerdict)) {
        colorCellService.lockForUpload(dongCode);
      }

      // location은 JPA 매핑 대상에서 제외되어 있다.
      // 따라서 우선 exif_verified=false로 저장한다.
      Photo photo =
          new Photo(
              user,
              dong,
              season,
              timeBand,
              takenAt,
              shotDate,
              false,
              aiVerdict,
              request.palette(),
              request.bins(),
              originalKey,
              thumbnailKey,
              imageHash,
              Boolean.TRUE.equals(request.isPublic()));

      try {
        photoRepository.saveAndFlush(photo);
      } catch (DataIntegrityViolationException e) {
        // 동시 요청의 image_hash UNIQUE 충돌만 중복 사진으로 처리한다.
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
          if (cause instanceof SQLException sql
              && sql.getErrorCode() == 1062
              && sql.getMessage() != null
              && sql.getMessage().contains("image_hash")) {
            throw new GeneralException(PhotoErrorCode.DUPLICATE_PHOTO);
          }
        }
        throw e;
      }

      if (latitude != null) {
        int updated =
            photoRepository.updateLocationAndVerification(
                photo.getPhotoId(), latitude, longitude, exifVerified);

        if (updated != 1) {
          throw new IllegalStateException("GPS 좌표 저장에 실패했습니다.");
        }
      }

      var contribution = colorCellService.aggregate(photo);

      return new PhotoUploadResponse(
          photo.getPhotoId(),
          dongCode,
          dong.getName(),
          season.getSeason(),
          timeBand.name(),
          exifVerified,
          photo.isPublic(),
          aiVerdict,
          moderation.toAiReason(),
          contribution.cellPhotoCount(),
          contribution.firstInCell());
    });
  }

  private void validateColors(PhotoUploadRequest request) {
    List<String> palette = request.palette();
    List<Double> bins = request.bins();

    if (palette == null || palette.size() != 5) {
      throw new GeneralException(PhotoErrorCode.INVALID_PALETTE_SIZE);
    }

    for (String color : palette) {
      if (color == null || !HEX_COLOR.matcher(color).matches()) {
        throw new GeneralException(PhotoErrorCode.INVALID_PALETTE_COLOR);
      }
    }

    if (bins == null || bins.size() != 40) {
      throw new GeneralException(PhotoErrorCode.INVALID_BINS_SIZE);
    }

    double sum = 0.0;

    for (Double value : bins) {
      if (value == null || !Double.isFinite(value) || value < 0.0 || value > 1.0) {
        throw new GeneralException(PhotoErrorCode.INVALID_BINS_VALUE);
      }
      sum += value;
    }

    if (Math.abs(sum - 1.0) > 0.02) {
      throw new GeneralException(PhotoErrorCode.INVALID_BINS_SUM);
    }
  }

  private void validateCoordinates(double latitude, double longitude) {
    if (!Double.isFinite(latitude)
        || !Double.isFinite(longitude)
        || latitude < -90
        || latitude > 90
        || longitude < -180
        || longitude > 180) {
      throw new GeneralException(PhotoErrorCode.INVALID_COORDINATES);
    }
  }

  private boolean nearlyEqual(double a, double b) {
    return Math.abs(a - b) < GPS_EPSILON;
  }

  private TimeBand resolveTimeBand(LocalTime time) {
    if (!time.isBefore(LocalTime.of(5, 0)) && time.isBefore(LocalTime.of(8, 0))) {
      return TimeBand.DAWN;
    }
    if (!time.isBefore(LocalTime.of(8, 0)) && time.isBefore(LocalTime.of(16, 0))) {
      return TimeBand.DAY;
    }
    if (!time.isBefore(LocalTime.of(16, 0)) && time.isBefore(LocalTime.of(19, 0))) {
      return TimeBand.SUNSET;
    }
    return TimeBand.NIGHT;
  }

  private String detectImageType(byte[] bytes) {
    if (bytes.length >= 3
        && (bytes[0] & 0xFF) == 0xFF
        && (bytes[1] & 0xFF) == 0xD8
        && (bytes[2] & 0xFF) == 0xFF) {
      return "jpg";
    }

    if (bytes.length >= 8
        && (bytes[0] & 0xFF) == 0x89
        && bytes[1] == 0x50
        && bytes[2] == 0x4E
        && bytes[3] == 0x47
        && bytes[4] == 0x0D
        && bytes[5] == 0x0A
        && bytes[6] == 0x1A
        && bytes[7] == 0x0A) {
      return "png";
    }

    throw new GeneralException(PhotoErrorCode.UNSUPPORTED_IMAGE_TYPE);
  }

  private String sha256(byte[] bytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(bytes));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
    }
  }
}
