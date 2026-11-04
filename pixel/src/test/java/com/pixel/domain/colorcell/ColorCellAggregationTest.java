package com.pixel.domain.colorcell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pixel.domain.colorcell.entity.ColorCell;
import com.pixel.domain.colorcell.enums.CellStatus;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.colorcell.service.ColorCellService;
import com.pixel.domain.colorcell.util.ColorBins;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.photo.entity.Photo;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    "spring.datasource.url=jdbc:h2:mem:aggregation;MODE=MySQL;NON_KEYWORDS=YEAR;"
        + "DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.test.database.replace=none"
})
@ActiveProfiles("test")
@Import(ColorCellService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ColorCellAggregationTest {

  @Autowired EntityManager em;
  @Autowired ColorCellService service;
  @Autowired ColorCellRepository cells;
  @Autowired PhotoRepository photos;
  @Autowired PlatformTransactionManager manager;
  TransactionTemplate tx;
  Long userId;
  Long otherUserId;
  Short seasonId;
  static final String DONG = "31050110";
  static final String OTHER_DONG = "31050120";
  static final LocalDate DAY = LocalDate.of(2026, 10, 29);

  @BeforeEach
  void setup() {
    tx = new TransactionTemplate(manager);
    tx.executeWithoutResult(status -> {
      cells.deleteAllInBatch();
      photos.deleteAllInBatch();
      em.createQuery("DELETE FROM User").executeUpdate();
      em.createQuery("DELETE FROM Season").executeUpdate();
      em.createQuery("DELETE FROM Dong").executeUpdate();
      for (String code : List.of(DONG, OTHER_DONG)) {
        var dong = BeanUtils.instantiateClass(Dong.class);
        ReflectionTestUtils.setField(dong, "dongCode", code);
        ReflectionTestUtils.setField(dong, "name", "테스트동");
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

  Photo photo(Long owner, LocalDate date, String dong, TimeBand band, boolean verified) {
    var bins = new ArrayList<>(Collections.nCopies(40, 0.0));
    bins.set(0, 1.0);
    return new Photo(em.getReference(User.class, owner), em.getReference(Dong.class, dong),
        em.getReference(Season.class, seasonId), band, date.atTime(13, 0), date, verified,
        Collections.nCopies(5, "#123ABC"), bins, "original", "thumbnail",
        UUID.randomUUID().toString(), false);
  }

  ColorCellService.Contribution upload(Long owner, LocalDate date, String dong, TimeBand band) {
    return tx.execute(status -> {
      service.lockForUpload(dong);
      var photo = photos.saveAndFlush(photo(owner, date, dong, band, true));
      return service.aggregate(photo);
    });
  }

  @Test
  void createsCellAndGroupsShotsByUserDateDongSeasonAndBand() {
    assertEquals(new ColorCellService.Contribution(1, true), upload(userId, DAY, DONG, TimeBand.DAY));
    assertEquals(new ColorCellService.Contribution(2, false), upload(userId, DAY, DONG, TimeBand.DAY));
    upload(userId, DAY.plusDays(1), DONG, TimeBand.DAY);
    upload(otherUserId, DAY, DONG, TimeBand.DAY);
    upload(userId, DAY, DONG, TimeBand.NIGHT);
    upload(userId, DAY, OTHER_DONG, TimeBand.DAY);
    tx.executeWithoutResult(status -> {
      var result = cells.findForMap(em.find(Season.class, seasonId), TimeBand.DAY);
      assertEquals(2, result.size());
      var cell = result.get(0);
      assertEquals(4, cell.getPhotoCount());
      assertEquals(3, cell.getShotCount());
      assertEquals(4.0, cell.getBinsSum().get(0), 1e-9);
      assertEquals(ColorBins.HEX.get(0), cell.getDominantColor());
      assertEquals(CellStatus.INSUFFICIENT, cell.getStatus());
      assertEquals(2, cells.findByDongAndSeason(em.find(Dong.class, DONG),
          em.find(Season.class, seasonId)).size());
      assertEquals(photos.findAll().stream().map(Photo::getPhotoId).min(Long::compare).orElseThrow(),
          cell.getFirstPhotoId());
    });
  }

  @Test
  void seededCellAndNativeVerificationRefreshAndWeight() {
    tx.executeWithoutResult(status -> cells.saveAndFlush(new ColorCell(em.find(Dong.class, DONG),
        em.find(Season.class, seasonId), TimeBand.DAY)));
    tx.executeWithoutResult(status -> {
      service.lockForUpload(DONG);
      var photo = photo(userId, DAY, DONG, TimeBand.DAY, false);
      ReflectionTestUtils.setField(photo, "weight", 0.5f);
      photos.saveAndFlush(photo);
      // H2에서는 공간 SQL 대신 검증 플래그의 네이티브 변경과 refresh 연결만 검증한다.
      em.createNativeQuery("UPDATE photos SET exif_verified = true WHERE photo_id = :id")
          .setParameter("id", photo.getPhotoId()).executeUpdate();
      assertFalse(photo.isExifVerified());
      assertEquals(new ColorCellService.Contribution(1, true), service.aggregate(photo));
    });
    tx.executeWithoutResult(status -> {
      assertEquals(1, cells.count());
      assertEquals(0.5, cells.findAll().get(0).getBinsSum().get(0), 1e-9);
    });
  }

  @Test
  void unverifiedPhotoDoesNotCreateOrChangeCell() {
    tx.executeWithoutResult(status -> {
      var photo = photos.saveAndFlush(photo(userId, DAY, DONG, TimeBand.DAY, false));
      assertEquals(new ColorCellService.Contribution(null, false), service.aggregate(photo));
      assertEquals(0, cells.count());
    });
  }

  @Test
  void concurrentUploadsDoNotLoseCountsOrCreateDuplicateCells() throws Exception {
    var executor = Executors.newFixedThreadPool(6);
    var start = new CountDownLatch(1);
    try {
      var futures = new ArrayList<java.util.concurrent.Future<ColorCellService.Contribution>>();
      for (int i = 0; i < 6; i++) {
        futures.add(executor.submit(() -> {
          assertTrue(start.await(10, TimeUnit.SECONDS));
          return upload(userId, DAY, DONG, TimeBand.DAY);
        }));
      }
      start.countDown();
      int first = 0;
      for (var future : futures) {
        if (future.get(30, TimeUnit.SECONDS).firstInCell()) {
          first++;
        }
      }
      assertEquals(1, first);
      tx.executeWithoutResult(status -> {
        assertEquals(1, cells.count());
        var cell = cells.findAll().get(0);
        assertEquals(6, cell.getPhotoCount());
        assertEquals(1, cell.getShotCount());
        assertEquals(6.0, cell.getBinsSum().get(0), 1e-9);
      });
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void aggregationFailureRollsBackPhotoAndCell() {
    assertThrows(IllegalArgumentException.class, () -> tx.executeWithoutResult(status -> {
      service.lockForUpload(DONG);
      var photo = photo(userId, DAY, DONG, TimeBand.DAY, true);
      ReflectionTestUtils.setField(photo, "bins", Collections.nCopies(40, -1.0));
      photos.saveAndFlush(photo);
      service.aggregate(photo);
    }));
    tx.executeWithoutResult(status -> {
      assertEquals(0, photos.count());
      assertEquals(0, cells.count());
    });
  }

  @Test
  void rollbackRestoresExistingCellAfterSuccessfulAggregation() {
    upload(userId, DAY, DONG, TimeBand.DAY);
    assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
      service.lockForUpload(DONG);
      var photo = photos.saveAndFlush(photo(otherUserId, DAY, DONG, TimeBand.DAY, true));
      assertEquals(new ColorCellService.Contribution(2, false), service.aggregate(photo));
      throw new IllegalStateException("집계 이후 실패");
    }));
    tx.executeWithoutResult(status -> {
      assertEquals(1, photos.count());
      var cell = cells.findAll().get(0);
      assertEquals(1, cell.getPhotoCount());
      assertEquals(1, cell.getShotCount());
      assertEquals(1.0, cell.getBinsSum().get(0), 1e-9);
      assertEquals(photos.findAll().get(0).getPhotoId(), cell.getFirstPhotoId());
    });
  }

  @Test
  void fiveDistinctShotsKeepExistingOkStatusRule() {
    for (int i = 0; i < 5; i++) {
      upload(userId, DAY.plusDays(i), DONG, TimeBand.DAY);
    }
    tx.executeWithoutResult(status -> assertEquals(CellStatus.OK, cells.findAll().get(0).getStatus()));
  }
}
