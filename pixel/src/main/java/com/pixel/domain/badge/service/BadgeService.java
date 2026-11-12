package com.pixel.domain.badge.service;

import com.pixel.domain.badge.dto.EarnedBadge;
import com.pixel.domain.badge.dto.MyBadgesResponse;
import com.pixel.domain.badge.enums.BadgeType;
import com.pixel.domain.badge.repository.UserBadgeRepository;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.photo.repository.PhotoRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BadgeService {

  static final int PHOTO_TEN_COUNT = 10;
  static final int DONG_FIVE_COUNT = 5;
  static final int DONG_TOP_RANK = 10;

  private final UserBadgeRepository badges;
  private final PhotoRepository photos;

  // 업로드 트랜잭션 안에서 사진 저장·집계가 끝난 뒤 부른다. 이번 업로드로 새로 받은 배지만 돌려준다.
  // countsForPublicMap: 이 사진이 모두의 지도에 들어갔는지 (Photo.countsForPublicMap과 같은 값)
  @Transactional(propagation = Propagation.MANDATORY)
  public List<BadgeType> awardForUpload(Photo photo, boolean countsForPublicMap) {
    Long userId = photo.getUser().getUserId();
    Set<BadgeType> owned = EnumSet.noneOf(BadgeType.class);
    owned.addAll(badges.findBadgeTypesByUserId(userId));
    List<BadgeType> awarded = new ArrayList<>();

    if (!owned.contains(BadgeType.FIRST_PHOTO)) {
      award(awarded, userId, BadgeType.FIRST_PHOTO, photo, null);
    }
    // 개수는 방금 저장한 사진을 포함한다.
    if (!owned.contains(BadgeType.PHOTO_TEN)
        && photos.countByUserUserId(userId) >= PHOTO_TEN_COUNT) {
      award(awarded, userId, BadgeType.PHOTO_TEN, photo, null);
    }
    if (!owned.contains(BadgeType.DONG_FIVE)
        && photos.countDistinctDongsByUserId(userId) >= DONG_FIVE_COUNT) {
      award(awarded, userId, BadgeType.DONG_FIVE, photo, null);
    }
    if (!owned.contains(BadgeType.ALL_TIME_BANDS)
        && photos.countDistinctTimeBandsByUserId(userId) >= TimeBand.values().length) {
      award(awarded, userId, BadgeType.ALL_TIME_BANDS, photo, null);
    }

    // 모두의 지도 배지: 같은 동·계절에서 몇 번째로 들어간 사진인지 본다.
    // 지운 사진은 세지 않으므로, 앞 사진이 지워지면 뒤에 올린 사람도 받을 수 있다.
    boolean needsRank =
        !owned.contains(BadgeType.DONG_FIRST) || !owned.contains(BadgeType.DONG_TOP10);
    if (countsForPublicMap && needsRank) {
      String dongCode = photo.getDong().getDongCode();
      long earlier = photos.countEarlierOnPublicMap(dongCode, photo.getSeason().getSeasonId(),
          photo.getPhotoId());
      if (earlier == 0 && !owned.contains(BadgeType.DONG_FIRST)) {
        award(awarded, userId, BadgeType.DONG_FIRST, photo, dongCode);
      }
      if (earlier < DONG_TOP_RANK && !owned.contains(BadgeType.DONG_TOP10)) {
        award(awarded, userId, BadgeType.DONG_TOP10, photo, dongCode);
      }
    }
    // 배지 목록 화면과 같은 순서(BadgeType 선언 순서)로 돌려준다.
    awarded.sort(null);
    return awarded;
  }

  @Transactional(readOnly = true)
  public MyBadgesResponse getMyBadges(Long userId) {
    Map<BadgeType, EarnedBadge> earned = new EnumMap<>(BadgeType.class);
    for (EarnedBadge badge : badges.findEarnedByUserId(userId)) {
      earned.put(badge.type(), badge);
    }
    List<MyBadgesResponse.Item> items = new ArrayList<>();
    for (BadgeType type : BadgeType.values()) {
      EarnedBadge badge = earned.get(type);
      items.add(badge == null
          ? new MyBadgesResponse.Item(type, false, null, null)
          : new MyBadgesResponse.Item(type, true, badge.earnedAt(), badge.dongName()));
    }
    return new MyBadgesResponse(items, earned.size(), BadgeType.values().length);
  }

  private void award(List<BadgeType> awarded, Long userId, BadgeType type, Photo photo,
      String dongCode) {
    // 호출 전에 보유 배지(owned)를 확인했으므로 여기서 넣는 배지는 새 배지다.
    // 같은 회원이 동시에 올려 둘 다 처음 받는 경우만 DB에는 한 번 들어가고 두 응답에 모두 표시된다.
    // 다른 엔티티의 created_at(BaseEntity)과 같이 JVM 시간(운영 TZ=Asia/Seoul)으로 남긴다.
    badges.insertIfAbsent(userId, type.name(), photo.getPhotoId(), dongCode, LocalDateTime.now());
    awarded.add(type);
  }
}
