package com.pixel.domain.badge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pixel.domain.badge.dto.MyBadgesResponse;
import com.pixel.domain.badge.enums.BadgeType;
import com.pixel.domain.badge.repository.UserBadgeRepository;
import com.pixel.domain.badge.service.BadgeService;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.photo.enums.AiVerdict;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:badge;MODE=MySQL;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1",
    "spring.test.database.replace=none"
})
@ActiveProfiles("test")
@Import(BadgeService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BadgeServiceTest {

  @Autowired EntityManager em;
  @Autowired BadgeService service;
  @Autowired UserBadgeRepository badges;
  @Autowired PhotoRepository photos;
  @Autowired PlatformTransactionManager manager;
  TransactionTemplate tx;
  Long userId;
  Long otherUserId;
  Short seasonId;
  static final List<String> DONGS =
      List.of("31050110", "31050120", "31050130", "31050140", "31050150");
  static final String DONG = DONGS.get(0);
  static final LocalDate DAY = LocalDate.of(2026, 10, 29);

  @BeforeEach
  void setup() {
    tx = new TransactionTemplate(manager);
    tx.executeWithoutResult(status -> {
      badges.deleteAllInBatch();
      photos.deleteAllInBatch();
      em.createQuery("DELETE FROM User").executeUpdate();
      em.createQuery("DELETE FROM Season").executeUpdate();
      em.createQuery("DELETE FROM Dong").executeUpdate();
      for (int i = 0; i < DONGS.size(); i++) {
        var dong = BeanUtils.instantiateClass(Dong.class);
        ReflectionTestUtils.setField(dong, "dongCode", DONGS.get(i));
        ReflectionTestUtils.setField(dong, "name", "테스트" + (i + 1) + "동");
        ReflectionTestUtils.setField(dong, "guName", "원미구");
        ReflectionTestUtils.setField(dong, "boundaryVersion", "20260101");
        em.persist(dong);
      }
      var season = BeanUtils.instantiateClass(Season.class);
      ReflectionTestUtils.setField(season, "year", (short) 2026);
      ReflectionTestUtils.setField(season, "season", SeasonType.AUTUMN);
      ReflectionTestUtils.setField(season, "startsOn", DAY.minusMonths(1));
      ReflectionTestUtils.setField(season, "endsOn", DAY.plusMonths(1));
      em.persist(season);
      seasonId = season.getSeasonId();
      userId = user("one").getUserId();
      otherUserId = user("two").getUserId();
    });
  }

  User user(String name) {
    var user = User.builder().email(name + "@test.invalid").passwordHash("test")
        .nickname(name).termsAgreedAt(LocalDateTime.now())
        .privacyAgreedAt(LocalDateTime.now()).build();
    em.persist(user);
    return user;
  }

  // 업로드처럼 사진을 저장하고 같은 트랜잭션에서 배지를 준다.
  List<BadgeType> upload(Long owner, String dong, TimeBand band, boolean publicMap) {
    return tx.execute(status -> {
      var bins = new ArrayList<>(Collections.nCopies(40, 0.0));
      bins.set(0, 1.0);
      var photo = photos.saveAndFlush(new Photo(em.find(User.class, owner),
          em.getReference(Dong.class, dong), em.getReference(Season.class, seasonId), band,
          DAY.atTime(13, 0), DAY, publicMap, AiVerdict.PUBLIC, Collections.nCopies(5, "#123ABC"),
          bins, "original", "thumbnail", UUID.randomUUID().toString(), false));
      return service.awardForUpload(photo, publicMap);
    });
  }

  @Test
  void firstUploadGivesFirstPhotoOnlyOnce() {
    assertEquals(List.of(BadgeType.FIRST_PHOTO), upload(userId, DONG, TimeBand.DAY, false));
    assertEquals(List.of(), upload(userId, DONG, TimeBand.DAY, false));
    assertEquals(1, badges.count());
  }

  @Test
  void duplicateInsertKeepsFirstRowWithoutError() {
    // 동시 업로드로 같은 배지를 두 번 넣어도 실패하지 않고 처음 행이 남는다.
    LocalDateTime first = LocalDateTime.of(2026, 11, 1, 9, 0);
    tx.executeWithoutResult(status -> {
      badges.insertIfAbsent(userId, BadgeType.FIRST_PHOTO.name(), null, null, first);
      badges.insertIfAbsent(userId, BadgeType.FIRST_PHOTO.name(), null, null, first.plusDays(1));
    });
    assertEquals(1, badges.count());
    assertEquals(first, badges.findAll().get(0).getCreatedAt());
  }

  @Test
  void firstPhotoOnPublicMapGivesPioneerAndTopTen() {
    assertEquals(List.of(BadgeType.FIRST_PHOTO, BadgeType.DONG_FIRST, BadgeType.DONG_TOP10),
        upload(userId, DONG, TimeBand.DAY, true));
    // 같은 동의 두 번째 사진: 다른 회원은 처음 10장 안이지만 개척자는 아님
    assertEquals(List.of(BadgeType.FIRST_PHOTO, BadgeType.DONG_TOP10),
        upload(otherUserId, DONG, TimeBand.NIGHT, true));
  }

  @Test
  void personalOnlyPhotoDoesNotCountForPublicMapBadges() {
    // 나의 지도에만 들어간 사진은 순서에 들지 않는다.
    assertEquals(List.of(BadgeType.FIRST_PHOTO), upload(userId, DONG, TimeBand.DAY, false));
    assertTrue(upload(otherUserId, DONG, TimeBand.DAY, true).contains(BadgeType.DONG_FIRST));
  }

  @Test
  void eleventhPhotoInDongMissesTopTen() {
    for (int i = 0; i < 10; i++) {
      upload(userId, DONG, TimeBand.DAY, true);
    }
    assertEquals(List.of(BadgeType.FIRST_PHOTO), upload(otherUserId, DONG, TimeBand.DAY, true));
  }

  @Test
  void tenPhotosFiveDongsAndAllTimeBands() {
    List<BadgeType> earned = new ArrayList<>();
    TimeBand[] bands = TimeBand.values();
    for (int i = 0; i < 10; i++) {
      earned.addAll(upload(userId, DONGS.get(i % DONGS.size()), bands[i % bands.length], false));
      if (i == 3) {
        assertTrue(earned.contains(BadgeType.ALL_TIME_BANDS));
        assertFalse(earned.contains(BadgeType.DONG_FIVE));
      }
      if (i == 4) {
        assertTrue(earned.contains(BadgeType.DONG_FIVE));
        assertFalse(earned.contains(BadgeType.PHOTO_TEN));
      }
    }
    assertTrue(earned.contains(BadgeType.PHOTO_TEN));
    assertEquals(4, earned.size());
  }

  @Test
  void badgesFromOneUploadComeInDisplayOrder() {
    TimeBand[] bands = TimeBand.values();
    for (int i = 0; i < 4; i++) {
      upload(userId, DONGS.get(i), bands[i], false);
    }
    // 다섯 번째 동이면서 그 동의 모두의 지도 첫 사진: 계산 순서와 달리 화면 순서로 온다.
    assertEquals(List.of(BadgeType.DONG_FIRST, BadgeType.DONG_TOP10, BadgeType.DONG_FIVE),
        upload(userId, DONGS.get(4), TimeBand.DAY, true));
  }

  @Test
  void myBadgesListsAllTypesInOrderWithDongName() {
    upload(userId, DONG, TimeBand.DAY, true);
    MyBadgesResponse response = service.getMyBadges(userId);
    assertEquals(3, response.earnedCount());
    assertEquals(BadgeType.values().length, response.totalCount());
    assertEquals(List.of(BadgeType.values()),
        response.badges().stream().map(MyBadgesResponse.Item::type).toList());
    var pioneer = response.badges().get(BadgeType.DONG_FIRST.ordinal());
    assertTrue(pioneer.earned());
    assertEquals("테스트1동", pioneer.dongName());
    var first = response.badges().get(BadgeType.FIRST_PHOTO.ordinal());
    assertNull(first.dongName());
    var locked = response.badges().get(BadgeType.PHOTO_TEN.ordinal());
    assertFalse(locked.earned());
    assertNull(locked.earnedAt());
  }

  @Test
  void deletedPhotoKeepsBadge() {
    upload(userId, DONG, TimeBand.DAY, true);
    tx.executeWithoutResult(status -> photos.deleteAll());
    assertEquals(3, service.getMyBadges(userId).earnedCount());
  }
}
