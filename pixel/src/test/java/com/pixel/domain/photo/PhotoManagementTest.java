package com.pixel.domain.photo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pixel.domain.colorcell.entity.ColorCell;
import com.pixel.domain.colorcell.enums.CellStatus;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.colorcell.service.ColorCellService;
import com.pixel.domain.colorcell.util.ColorBins;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.photo.enums.AiVerdict;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.photo.service.PhotoManagementService;
import com.pixel.domain.region.entity.Region;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.user.entity.User;
import com.pixel.global.security.jwt.JwtTokenProvider;
import com.pixel.global.storage.S3StorageService;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:management;MODE=MySQL;"
    + "NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhotoManagementTest {
  @Autowired MockMvc mvc;
  @Autowired PhotoManagementService management;
  @Autowired JwtTokenProvider jwt;
  @MockitoBean S3StorageService storage;
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
    when(storage.readUrl(anyString())).thenReturn("https://test.invalid/image");
    tx = new TransactionTemplate(manager);
    tx.executeWithoutResult(status -> {
      cells.deleteAllInBatch();
      photos.deleteAllInBatch();
      em.createQuery("DELETE FROM User").executeUpdate();
      em.createQuery("DELETE FROM Season").executeUpdate();
      em.createQuery("DELETE FROM Dong").executeUpdate();
      em.createQuery("DELETE FROM Region").executeUpdate();
      var region = BeanUtils.instantiateClass(Region.class);
      ReflectionTestUtils.setField(region, "regionCode", "3105");
      ReflectionTestUtils.setField(region, "sidoName", "경기도");
      ReflectionTestUtils.setField(region, "name", "부천시");
      em.persist(region);
      for (String code : List.of(DONG, OTHER_DONG)) {
        var dong = BeanUtils.instantiateClass(Dong.class);
        ReflectionTestUtils.setField(dong, "dongCode", code);
        ReflectionTestUtils.setField(dong, "name", "테스트동");
        ReflectionTestUtils.setField(dong, "guName", "원미구");
        ReflectionTestUtils.setField(dong, "boundaryVersion", "20260101");
        ReflectionTestUtils.setField(dong, "region", region);
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
    return photo(owner, date, dong, band, verified, AiVerdict.PUBLIC);
  }

  Photo photo(Long owner, LocalDate date, String dong, TimeBand band, boolean verified,
      AiVerdict verdict) {
    var bins = new ArrayList<>(Collections.nCopies(40, 0.0));
    bins.set(0, 1.0);
    return new Photo(em.getReference(User.class, owner), em.getReference(Dong.class, dong),
        em.getReference(Season.class, seasonId), band, date.atTime(13, 0), date, verified,
        verdict, Collections.nCopies(5, "#123ABC"), bins, "original", "thumbnail",
        UUID.randomUUID().toString(), false);
  }

  Long upload(Long owner, LocalDate day, boolean verified) {
    return upload(owner, day, verified, AiVerdict.PUBLIC);
  }

  Long upload(Long owner, LocalDate day, boolean verified, AiVerdict verdict) {
    return tx.execute(status -> {
      service.lockForUpload(DONG);
      var photo = photos.saveAndFlush(photo(owner, day, DONG, TimeBand.DAY, verified, verdict));
      em.createNativeQuery("UPDATE photos SET created_at = :time WHERE photo_id = :id")
          .setParameter("time", DAY.atStartOfDay()).setParameter("id", photo.getPhotoId())
          .executeUpdate();
      service.aggregate(photo);
      return photo.getPhotoId();
    });
  }

  String token(Long id) {
    return "Bearer " + jwt.createAccessToken(id);
  }

  @Test
  void detailPermissions() throws Exception {
    Long id = upload(userId, DAY, true);
    mvc.perform(get("/api/photos/" + id)).andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("COMMON401"));
    verify(storage, never()).readUrl(anyString());
    mvc.perform(get("/api/photos/" + id).header("Authorization", token(otherUserId)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/photos/" + id).header("Authorization", token(userId)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("PHOTO200_1"))
        .andExpect(jsonPath("$.result.name").value("테스트동"))
        .andExpect(jsonPath("$.result.bins.length()").value(40))
        .andExpect(jsonPath("$.result.bins[0]").value(1.0))
        .andExpect(jsonPath("$.result.bins[1]").value(0.0));
    management.changeVisibility(id, userId, true);
    mvc.perform(get("/api/photos/" + id)).andExpect(status().isOk());
    mvc.perform(get("/api/photos/999999")).andExpect(status().isNotFound());
  }

  @Test
  void listPaginationIncludesPrivateAndExcludesOthers() throws Exception {
    upload(userId, DAY, false);
    Long newest = upload(userId, DAY, false);
    upload(otherUserId, DAY, false);
    mvc.perform(get("/api/users/me/photos")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/users/me/photos?size=1").header("Authorization", token(userId)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("PHOTO200_4"))
        .andExpect(jsonPath("$.result.totalElements").value(2))
        .andExpect(jsonPath("$.result.photos[0].color").value("#123ABC"))
        .andExpect(jsonPath("$.result.photos[0].photoId").value(newest))
        .andExpect(jsonPath("$.result.photos[0].isPublic").value(false))
        .andExpect(jsonPath("$.result.photos[0].aiVerdict").value("PUBLIC"))
        .andExpect(jsonPath("$.result.hasNext").value(true));
    mvc.perform(get("/api/users/me/photos?size=1&page=1").header("Authorization", token(userId)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.result.hasNext").value(false));
    for (String query : List.of("page=-1", "size=0", "size=101", "page=no")) {
      mvc.perform(get("/api/users/me/photos?" + query).header("Authorization", token(userId)))
          .andExpect(status().isBadRequest());
    }
  }

  @Test
  void visibilityValidationAndAggregationUnchanged() throws Exception {
    Long id = upload(userId, DAY, true);
    mvc.perform(patch("/api/photos/" + id).header("Authorization", token(otherUserId))
        .contentType("application/json").content("{\"isPublic\":true}"))
        .andExpect(status().isForbidden());
    mvc.perform(patch("/api/photos/" + id).header("Authorization", token(userId))
        .contentType("application/json").content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(patch("/api/photos/" + id).header("Authorization", token(userId))
        .contentType("application/json").content("{\"isPublic\":true}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("PHOTO200_2"));
    tx.executeWithoutResult(status -> {
      assertTrue(photos.findById(id).orElseThrow().isPublic());
      var cell = cells.findAll().get(0);
      assertEquals(1, cell.getPhotoCount());
      assertEquals(1, cell.getShotCount());
      assertEquals(1.0, cell.getBinsSum().get(0));
      assertEquals(id, cell.getFirstPhotoId());
    });
  }

  @Test
  void notPublicVerdictPhotoCannotBePublishedButCanStayPrivate() throws Exception {
    for (AiVerdict verdict : List.of(AiVerdict.PERSONAL_ONLY, AiVerdict.UNCHECKED)) {
      Long id = upload(userId, DAY, true, verdict);
      mvc.perform(patch("/api/photos/" + id).header("Authorization", token(userId))
          .contentType("application/json").content("{\"isPublic\":true}"))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("PHOTO403_2"));
      mvc.perform(patch("/api/photos/" + id).header("Authorization", token(userId))
          .contentType("application/json").content("{\"isPublic\":false}"))
          .andExpect(status().isOk());
      tx.executeWithoutResult(status -> assertFalse(photos.findById(id).orElseThrow().isPublic()));
      // 비공개 사진이라 다른 사용자는 상세를 볼 수 없다.
      mvc.perform(get("/api/photos/" + id).header("Authorization", token(otherUserId)))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void deletingPersonalOnlyPhotoKeepsPublicCell() {
    Long kept = upload(userId, DAY, true);
    Long personal = upload(userId, DAY, true, AiVerdict.PERSONAL_ONLY);
    management.delete(personal, userId);
    tx.executeWithoutResult(status -> {
      var cell = cells.findAll().get(0);
      assertEquals(1, cell.getPhotoCount());
      assertEquals(1, cell.getShotCount());
      assertEquals(1.0, cell.getBinsSum().get(0));
      assertEquals(kept, cell.getFirstPhotoId());
    });
  }

  @Test
  void deleteFirstAndLastReversesCountsAndFirstPhoto() throws Exception {
    Long first = upload(userId, DAY, true);
    Long second = upload(userId, DAY, true);
    mvc.perform(delete("/api/photos/" + first).header("Authorization", token(userId)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("PHOTO200_3"));
    tx.executeWithoutResult(status -> {
      var cell = cells.findAll().get(0);
      assertEquals(1, cell.getPhotoCount());
      assertEquals(1, cell.getShotCount());
      assertEquals(second, cell.getFirstPhotoId());
      assertEquals(1.0, cell.getBinsSum().get(0));
    });
    management.delete(second, userId);
    tx.executeWithoutResult(status -> {
      var cell = cells.findAll().get(0);
      assertEquals(0, cell.getPhotoCount());
      assertEquals(0, cell.getShotCount());
      assertNull(cell.getFirstPhotoId());
      assertNull(cell.getDominantColor());
      assertTrue(cell.getBinsSum().stream().allMatch(value -> value == 0));
    });
    verify(storage, times(2)).delete("original");
    verify(storage, times(2)).delete("thumbnail");
  }

  @Test
  void separateShotsAndUnverifiedDeletion() {
    Long first = upload(userId, DAY, true);
    upload(otherUserId, DAY, true);
    Long unverified = upload(userId, DAY, false);
    management.delete(unverified, userId);
    tx.executeWithoutResult(status -> assertEquals(2, cells.findAll().get(0).getShotCount()));
    management.delete(first, userId);
    tx.executeWithoutResult(status -> assertEquals(1, cells.findAll().get(0).getShotCount()));
  }

  @Test
  void rollbackPreservesFilesAndAggregate() {
    Long id = upload(userId, DAY, true);
    tx.executeWithoutResult(status -> {
      management.delete(id, userId);
      status.setRollbackOnly();
    });
    assertTrue(photos.existsById(id));
    tx.executeWithoutResult(status -> assertEquals(1, cells.findAll().get(0).getPhotoCount()));
    verify(storage, never()).delete(anyString());
  }

  @Test
  void storageFailureRetriesWithoutUndoingCommittedDatabase() {
    Long id = upload(userId, DAY, true);
    doThrow(new IllegalStateException("S3 unavailable")).when(storage).delete("original");
    management.delete(id, userId);
    assertFalse(photos.existsById(id));
    verify(storage, times(3)).delete("original");
    verify(storage).delete("thumbnail");
  }

  @Test
  void deleteRequiresOwner() throws Exception {
    Long id = upload(userId, DAY, true);
    mvc.perform(delete("/api/photos/" + id)).andExpect(status().isUnauthorized());
    mvc.perform(delete("/api/photos/" + id).header("Authorization", token(otherUserId)))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/api/photos/999999").header("Authorization", token(userId)))
        .andExpect(status().isNotFound());
    assertTrue(photos.existsById(id));
    verify(storage, never()).delete(anyString());
  }

  @Test
  void concurrentUploadAndDeletePreserveCell() throws Exception {
    Long id = upload(userId, DAY, true);
    var executor = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      var deletion = executor.submit(() -> {
        start.await();
        management.delete(id, userId);
        return null;
      });
      var addition = executor.submit(() -> {
        start.await();
        return upload(userId, DAY, true);
      });
      start.countDown();
      deletion.get(30, TimeUnit.SECONDS);
      Long remaining = addition.get(30, TimeUnit.SECONDS);
      tx.executeWithoutResult(status -> {
        var cell = cells.findAll().get(0);
        assertEquals(1, cell.getPhotoCount());
        assertEquals(1, cell.getShotCount());
        assertEquals(remaining, cell.getFirstPhotoId());
      });
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void weightedDeletionRecalculatesDominantColor() {
    Long first = upload(userId, DAY, true);
    Long second = tx.execute(status -> {
      service.lockForUpload(DONG);
      Photo next = photo(userId, DAY, DONG, TimeBand.DAY, true);
      var bins = new ArrayList<>(Collections.nCopies(40, 0.0));
      bins.set(3, 1.0);
      ReflectionTestUtils.setField(next, "bins", bins);
      ReflectionTestUtils.setField(next, "weight", 0.5f);
      photos.saveAndFlush(next);
      service.aggregate(next);
      return next.getPhotoId();
    });
    management.delete(first, userId);
    tx.executeWithoutResult(status -> {
      var cell = cells.findAll().get(0);
      assertEquals(0.0, cell.getBinsSum().get(0));
      assertEquals(0.5, cell.getBinsSum().get(3));
      assertEquals(ColorBins.HEX.get(3), cell.getDominantColor());
      assertEquals(second, cell.getFirstPhotoId());
    });
  }

  @Test
  void corruptAggregateRollsBackDeletion() {
    Long id = upload(userId, DAY, true);
    tx.executeWithoutResult(status -> cells.deleteAllInBatch());
    assertThrows(IllegalStateException.class, () -> management.delete(id, userId));
    assertTrue(photos.existsById(id));
    verify(storage, never()).delete(anyString());
  }

  @Test
  void concurrentDeletionDoesNotSubtractTwice() throws Exception {
    Long id = upload(userId, DAY, true);
    var executor = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    java.util.concurrent.Callable<Boolean> deletion = () -> {
      start.await();
      try {
        management.delete(id, userId);
        return true;
      } catch (com.pixel.global.apiPayload.exception.GeneralException ex) {
        return false;
      }
    };
    try {
      var first = executor.submit(deletion);
      var second = executor.submit(deletion);
      start.countDown();
      assertTrue(first.get(30, TimeUnit.SECONDS) ^ second.get(30, TimeUnit.SECONDS));
      tx.executeWithoutResult(status -> {
        assertEquals(0, cells.findAll().get(0).getPhotoCount());
        assertEquals(0, cells.findAll().get(0).getShotCount());
      });
      verify(storage).delete("original");
      verify(storage).delete("thumbnail");
    } finally {
      executor.shutdownNow();
    }
  }
}
