package com.pixel.domain.photo.service;

import com.pixel.domain.colorcell.service.ColorCellService;
import com.pixel.domain.photo.dto.LikedPhotosResponse;
import com.pixel.domain.photo.dto.MyPhotosResponse;
import com.pixel.domain.photo.dto.PhotoDetailResponse;
import com.pixel.domain.photo.dto.PhotoLikeResponse;
import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.photo.exception.code.PhotoErrorCode;
import com.pixel.domain.photo.repository.PhotoLikeRepository;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.user.enums.UserStatus;
import com.pixel.domain.user.exception.code.UserErrorCode;
import com.pixel.domain.user.repository.UserRepository;
import com.pixel.global.apiPayload.exception.GeneralException;
import com.pixel.global.apiPayload.status.ErrorStatus;
import com.pixel.global.storage.S3StorageService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoManagementService {

  private final PhotoRepository photos;
  private final PhotoLikeRepository likes;
  private final UserRepository users;
  private final ColorCellService cells;
  private final S3StorageService storage;

  public PhotoDetailResponse detail(Long photoId, Long userId) {
    Photo photo = photos.findById(photoId)
        .orElseThrow(() -> new GeneralException(PhotoErrorCode.NOT_FOUND));
    if (!photo.isPublic()) {
      requireActiveUser(userId);
      requireOwner(photo, userId);
    }
    return new PhotoDetailResponse(photo.getPhotoId(), storage.readUrl(photo.getImageKey()),
        photo.getUser().getNickname(), photo.getDong().getName(), photo.getTakenAt(),
        photo.getTimeBand(), photo.getPalette(), photo.getBins(),
        likes.countByPhotoId(photoId),
        userId != null && likes.existsByUserIdAndPhotoId(userId, photoId));
  }

  // 볼 수 있는 사진(공개 사진이거나 내 사진)에만 누를 수 있다. 이미 눌렀으면 그대로 둔다.
  @Transactional
  public PhotoLikeResponse like(Long photoId, Long userId) {
    requireActiveUser(userId);
    Photo photo = photos.findById(photoId)
        .orElseThrow(() -> new GeneralException(PhotoErrorCode.NOT_FOUND));
    if (!photo.isPublic()) {
      requireOwner(photo, userId);
    }
    // 다른 엔티티의 created_at(BaseEntity)과 같이 JVM 시간(운영 TZ=Asia/Seoul)으로 남긴다.
    likes.insertIfAbsent(userId, photoId, LocalDateTime.now());
    return new PhotoLikeResponse(photoId, true, likes.countByPhotoId(photoId));
  }

  // 사진이 비공개로 바뀌어도 내가 누른 좋아요는 취소할 수 있다. 누르지 않았으면 그대로 둔다.
  @Transactional
  public PhotoLikeResponse unlike(Long photoId, Long userId) {
    requireActiveUser(userId);
    if (!photos.existsById(photoId)) {
      throw new GeneralException(PhotoErrorCode.NOT_FOUND);
    }
    likes.deleteByUserIdAndPhotoId(userId, photoId);
    return new PhotoLikeResponse(photoId, false, likes.countByPhotoId(photoId));
  }

  public LikedPhotosResponse likedPhotos(Long userId, int page, int size) {
    requireActiveUser(userId);
    if (page < 0 || size < 1 || size > 100) {
      throw new GeneralException(ErrorStatus._BAD_REQUEST);
    }
    var result = likes.findLikedPhotos(userId, PageRequest.of(page, size));
    var items = result.map(photo -> new LikedPhotosResponse.Item(photo.getPhotoId(),
        storage.readUrl(photo.getThumbnailKey()), photo.getDong().getDongCode(),
        photo.getDong().getName(), photo.getUser().getNickname(), photo.getTimeBand(),
        photo.getTakenAt(), photo.getPalette().get(0))).getContent();
    return new LikedPhotosResponse(items, page, size, result.getTotalElements(),
        result.hasNext());
  }

  public MyPhotosResponse myPhotos(Long userId, int page, int size) {
    requireActiveUser(userId);
    if (page < 0 || size < 1 || size > 100) {
      throw new GeneralException(ErrorStatus._BAD_REQUEST);
    }
    var result = photos.findByUserUserId(userId, PageRequest.of(page, size,
        Sort.by(Sort.Direction.DESC, "createdAt", "photoId")));
    var items = result.map(photo -> new MyPhotosResponse.Item(photo.getPhotoId(),
        storage.readUrl(photo.getThumbnailKey()), photo.getDong().getDongCode(),
        photo.getDong().getName(), photo.getTimeBand(), photo.getTakenAt(),
        // 사진의 점 색은 palette[0], 지도 셀의 집계 색은 bins 기준이다.
        photo.getPalette().get(0), photo.isPublic(), photo.getAiVerdict())).getContent();
    return new MyPhotosResponse(items, page, size, result.getTotalElements(), result.hasNext());
  }

  @Transactional
  public void changeVisibility(Long photoId, Long userId, boolean isPublic) {
    requireActiveUser(userId);
    Photo photo = lockedPhoto(photoId);
    requireOwner(photo, userId);
    // 풍경이 아니거나 얼굴·번호판이 크게 나온 사진, 검수하지 못한 사진은 공개로 바꿀 수 없다.
    if (isPublic && !photo.canBePublic()) {
      throw new GeneralException(PhotoErrorCode.NOT_PUBLISHABLE);
    }
    photo.changePublic(isPublic);
  }

  @Transactional
  public void delete(Long photoId, Long userId) {
    requireActiveUser(userId);
    // 엔티티를 먼저 읽으면 잠금을 기다리는 동안 낡은 상태가 캐시에 남을 수 있다.
    String dongCode = photos.findDongCodeByPhotoId(photoId)
        .orElseThrow(() -> new GeneralException(PhotoErrorCode.NOT_FOUND));
    cells.lockForUpload(dongCode);
    Photo photo = lockedPhoto(photoId);
    requireOwner(photo, userId);
    cells.retract(photo);
    List<String> keys = List.of(photo.getImageKey(), photo.getThumbnailKey());
    // 운영 DB는 FK ON DELETE CASCADE로도 지우지만, 테이블 정의와 상관없이 같은 결과가 나오도록 먼저 지운다.
    likes.deleteAllByPhotoId(photoId);
    photos.delete(photo);
    photos.flush();
    // DB 롤백 시 파일을 보존한다. S3 실패로 이미 커밋된 DB가 롤백된 것처럼 응답하지 않는다.
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        keys.forEach(key -> deleteStorageObject(photoId, key));
      }
    });
  }

  private Photo lockedPhoto(Long photoId) {
    return photos.findForUpdate(photoId)
        .orElseThrow(() -> new GeneralException(PhotoErrorCode.NOT_FOUND));
  }

  private void requireOwner(Photo photo, Long userId) {
    if (userId == null || !photo.getUser().getUserId().equals(userId)) {
      throw new GeneralException(PhotoErrorCode.FORBIDDEN);
    }
  }

  private void requireActiveUser(Long userId) {
    if (userId == null) {
      throw new GeneralException(ErrorStatus._UNAUTHORIZED);
    }
    var user = users.findById(userId)
        .orElseThrow(() -> new GeneralException(UserErrorCode._NOT_FOUND));
    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new GeneralException(UserErrorCode.INACTIVE_USER);
    }
  }

  private void deleteStorageObject(Long photoId, String key) {
    for (int attempt = 1; attempt <= 3; attempt++) {
      try {
        storage.delete(key);
        return;
      } catch (RuntimeException ex) {
        if (attempt == 3) {
          log.error("S3 사진 정리 실패: photoId={}, key={}. 수동 재처리가 필요합니다.",
              photoId, key, ex);
        }
      }
    }
  }
}
