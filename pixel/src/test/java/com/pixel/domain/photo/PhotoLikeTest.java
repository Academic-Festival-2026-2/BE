package com.pixel.domain.photo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.colorcell.service.ColorCellService;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.photo.dto.PhotoLikeResponse;
import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.photo.enums.AiVerdict;
import com.pixel.domain.photo.repository.PhotoLikeRepository;
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:photolike;MODE=MySQL;"
    + "NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhotoLikeTest {
  @Autowired MockMvc mvc;
  @Autowired PhotoManagementService management;
  @Autowired JwtTokenProvider jwt;
  @MockitoBean S3StorageService storage;
  @Autowired EntityManager em;
  @Autowired ColorCellService cellService;
  @Autowired ColorCellRepository cells;
  @Autowired PhotoRepository photos;
  @Autowired PhotoLikeRepository likes;
  @Autowired PlatformTransactionManager manager;
  @Autowired PasswordEncoder passwordEncoder;
  TransactionTemplate tx;
  Long ownerId;
  Long likerId;
  Long thirdId;
  Short seasonId;
  static final String DONG = "31050110";
  static final LocalDate DAY = LocalDate.of(2026, 10, 29);

  @BeforeEach
  void setup() {
    when(storage.readUrl(anyString())).thenReturn("https://test.invalid/image");
    tx = new TransactionTemplate(manager);
    tx.executeWithoutResult(status -> {
      likes.deleteAllInBatch();
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
      var dong = BeanUtils.instantiateClass(Dong.class);
      ReflectionTestUtils.setField(dong, "dongCode", DONG);
      ReflectionTestUtils.setField(dong, "name", "테스트동");
      ReflectionTestUtils.setField(dong, "guName", "원미구");
      ReflectionTestUtils.setField(dong, "boundaryVersion", "20260101");
      ReflectionTestUtils.setField(dong, "region", region);
      em.persist(dong);
      var season = BeanUtils.instantiateClass(Season.class);
      ReflectionTestUtils.setField(season, "season", SeasonType.AUTUMN);
      em.persist(season);
      seasonId = season.getSeasonId();
      ownerId = user("owner").getUserId();
      likerId = user("liker").getUserId();
      thirdId = user("third").getUserId();
    });
  }

  User user(String name) {
    var user = User.builder().email(name + "@test.invalid").passwordHash("test")
        .nickname(name).termsAgreedAt(LocalDateTime.now())
        .privacyAgreedAt(LocalDateTime.now()).build();
    em.persist(user);
    return user;
  }

  // 공개 여부를 정해 사진을 올린다. 칸 집계까지 해 두어야 삭제(retract)가 맞게 돈다.
  Long upload(Long owner, boolean isPublic) {
    Long id = tx.execute(status -> {
      cellService.lockForUpload(DONG);
      var bins = new ArrayList<>(Collections.nCopies(40, 0.0));
      bins.set(0, 1.0);
      var photo = photos.saveAndFlush(new Photo(em.getReference(User.class, owner),
          em.getReference(Dong.class, DONG), em.getReference(Season.class, seasonId),
          TimeBand.DAY, DAY.atTime(13, 0), DAY, true, AiVerdict.PUBLIC,
          Collections.nCopies(5, "#123ABC"), bins, "original", "thumbnail", "display",
          UUID.randomUUID().toString(), false));
      cellService.aggregate(photo);
      return photo.getPhotoId();
    });
    if (isPublic) {
      management.changeVisibility(id, owner, true);
    }
    return id;
  }

  String token(Long id) {
    return "Bearer " + jwt.createAccessToken(id);
  }

  void like(Long photoId, Long userId) throws Exception {
    mvc.perform(post("/api/photos/" + photoId + "/likes").header("Authorization", token(userId)))
        .andExpect(status().isOk());
  }

  @Test
  void likeAndUnlikeArePerUserAndIdempotent() throws Exception {
    Long id = upload(ownerId, true);
    mvc.perform(post("/api/photos/" + id + "/likes")).andExpect(status().isUnauthorized());

    mvc.perform(post("/api/photos/" + id + "/likes").header("Authorization", token(likerId)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("PHOTO200_5"))
        .andExpect(jsonPath("$.result.photoId").value(id))
        .andExpect(jsonPath("$.result.liked").value(true))
        .andExpect(jsonPath("$.result.likeCount").value(1));
    // 다시 눌러도 한 번만 센다
    mvc.perform(post("/api/photos/" + id + "/likes").header("Authorization", token(likerId)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.result.likeCount").value(1));
    // 내 사진에도 누를 수 있다
    mvc.perform(post("/api/photos/" + id + "/likes").header("Authorization", token(ownerId)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.result.likeCount").value(2));

    mvc.perform(get("/api/photos/" + id)).andExpect(status().isOk())
        .andExpect(jsonPath("$.result.likeCount").value(2))
        .andExpect(jsonPath("$.result.liked").value(false));
    mvc.perform(get("/api/photos/" + id).header("Authorization", token(likerId)))
        .andExpect(jsonPath("$.result.liked").value(true));
    mvc.perform(get("/api/photos/" + id).header("Authorization", token(thirdId)))
        .andExpect(jsonPath("$.result.liked").value(false));

    mvc.perform(delete("/api/photos/" + id + "/likes").header("Authorization", token(likerId)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("PHOTO200_6"))
        .andExpect(jsonPath("$.result.liked").value(false))
        .andExpect(jsonPath("$.result.likeCount").value(1));
    // 누르지 않은 상태에서 취소해도 그대로
    mvc.perform(delete("/api/photos/" + id + "/likes").header("Authorization", token(likerId)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.result.likeCount").value(1));
    mvc.perform(get("/api/photos/" + id).header("Authorization", token(likerId)))
        .andExpect(jsonPath("$.result.liked").value(false));

    mvc.perform(delete("/api/photos/" + id + "/likes")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/photos/999999/likes").header("Authorization", token(likerId)))
        .andExpect(status().isNotFound());
    mvc.perform(delete("/api/photos/999999/likes").header("Authorization", token(likerId)))
        .andExpect(status().isNotFound());
  }

  @Test
  void concurrentLikesAreCountedOnceEach() throws Exception {
    Long id = upload(ownerId, true);
    // 같은 회원이 두 번 + 다른 두 회원이 동시에 누름 → 3명분만 남고, 응답 개수에 자기 좋아요가 빠지지 않음
    List<Long> users = List.of(likerId, likerId, thirdId, ownerId);
    var start = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(users.size());
    try {
      List<Future<PhotoLikeResponse>> results = new ArrayList<>();
      for (Long user : users) {
        results.add(pool.submit(() -> {
          start.await();
          return management.like(id, user);
        }));
      }
      start.countDown();
      for (var result : results) {
        PhotoLikeResponse response = result.get(10, TimeUnit.SECONDS);
        assertTrue(response.liked());
        assertTrue(response.likeCount() >= 1 && response.likeCount() <= 3);
      }
    } finally {
      pool.shutdownNow();
    }
    assertEquals(3, likes.countByPhotoId(id));
  }

  @Test
  void privatePhotoCanBeLikedOnlyByOwner() throws Exception {
    Long id = upload(ownerId, false);
    mvc.perform(post("/api/photos/" + id + "/likes").header("Authorization", token(likerId)))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("PHOTO403_1"));
    mvc.perform(post("/api/photos/" + id + "/likes").header("Authorization", token(ownerId)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.result.likeCount").value(1));
  }

  @Test
  void likedPhotosListsVisiblePhotosNewestLikeFirst() throws Exception {
    Long first = upload(ownerId, true);
    Long second = upload(ownerId, true);
    Long laterPrivate = upload(ownerId, true);
    Long mine = upload(likerId, false);
    Long othersOnly = upload(ownerId, true);
    // 누른 순서: second → first → laterPrivate → mine. othersOnly는 다른 회원만 누름
    for (Long id : List.of(second, first, laterPrivate, mine)) {
      like(id, likerId);
    }
    like(othersOnly, thirdId);
    // 좋아요를 누른 뒤 주인이 비공개로 바꾸면 목록에서 빠진다
    management.changeVisibility(laterPrivate, ownerId, false);

    mvc.perform(get("/api/users/me/liked-photos")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/users/me/liked-photos").header("Authorization", token(likerId)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("PHOTO200_7"))
        .andExpect(jsonPath("$.result.totalElements").value(3))
        .andExpect(jsonPath("$.result.photos.length()").value(3))
        .andExpect(jsonPath("$.result.photos[0].photoId").value(mine))
        .andExpect(jsonPath("$.result.photos[0].nickname").value("liker"))
        .andExpect(jsonPath("$.result.photos[1].photoId").value(first))
        .andExpect(jsonPath("$.result.photos[1].nickname").value("owner"))
        .andExpect(jsonPath("$.result.photos[1].name").value("테스트동"))
        .andExpect(jsonPath("$.result.photos[1].dongCode").value(DONG))
        .andExpect(jsonPath("$.result.photos[1].color").value("#123ABC"))
        .andExpect(jsonPath("$.result.photos[1].timeBand").value("DAY"))
        .andExpect(jsonPath("$.result.photos[1].thumbnailUrl")
            .value("https://test.invalid/image"))
        .andExpect(jsonPath("$.result.photos[2].photoId").value(second))
        .andExpect(jsonPath("$.result.hasNext").value(false));

    mvc.perform(get("/api/users/me/liked-photos?size=2").header("Authorization", token(likerId)))
        .andExpect(jsonPath("$.result.photos.length()").value(2))
        .andExpect(jsonPath("$.result.hasNext").value(true));
    mvc.perform(get("/api/users/me/liked-photos?size=2&page=1")
            .header("Authorization", token(likerId)))
        .andExpect(jsonPath("$.result.photos[0].photoId").value(second))
        .andExpect(jsonPath("$.result.hasNext").value(false));
    for (String query : List.of("page=-1", "size=0", "size=101", "page=no")) {
      mvc.perform(get("/api/users/me/liked-photos?" + query)
          .header("Authorization", token(likerId))).andExpect(status().isBadRequest());
    }

    // 다시 공개하면 목록에 돌아온다
    management.changeVisibility(laterPrivate, ownerId, true);
    mvc.perform(get("/api/users/me/liked-photos").header("Authorization", token(likerId)))
        .andExpect(jsonPath("$.result.totalElements").value(4))
        .andExpect(jsonPath("$.result.photos[1].photoId").value(laterPrivate));
  }

  @Test
  void deletingPhotoRemovesItsLikes() throws Exception {
    Long id = upload(ownerId, true);
    Long kept = upload(ownerId, true);
    like(id, likerId);
    like(kept, likerId);
    mvc.perform(delete("/api/photos/" + id).header("Authorization", token(ownerId)))
        .andExpect(status().isOk());
    assertEquals(0, likes.countByPhotoId(id));
    assertEquals(1, likes.countByPhotoId(kept));
    mvc.perform(get("/api/users/me/liked-photos").header("Authorization", token(likerId)))
        .andExpect(jsonPath("$.result.totalElements").value(1))
        .andExpect(jsonPath("$.result.photos[0].photoId").value(kept));
  }

  @Test
  void withdrawingRemovesTheirLikes() throws Exception {
    Long id = upload(ownerId, true);
    Long other = upload(ownerId, true);
    Long leaverId = tx.execute(status -> {
      var leaver = user("leaver");
      ReflectionTestUtils.setField(leaver, "passwordHash", passwordEncoder.encode("password123"));
      return leaver.getUserId();
    });
    like(id, leaverId);
    like(other, leaverId);
    like(id, likerId);

    mvc.perform(delete("/api/users/me").header("Authorization", token(leaverId))
            .contentType(MediaType.APPLICATION_JSON).content("{\"password\": \"password123\"}"))
        .andExpect(status().isOk());

    // 탈퇴한 회원의 좋아요만 빠지고 다른 회원의 좋아요는 그대로
    assertEquals(1, likes.countByPhotoId(id));
    assertEquals(0, likes.countByPhotoId(other));
    mvc.perform(get("/api/photos/" + id)).andExpect(jsonPath("$.result.likeCount").value(1));
    // 남은 access token으로도 다시 누를 수 없다
    mvc.perform(post("/api/photos/" + other + "/likes").header("Authorization", token(leaverId)))
        .andExpect(status().isUnauthorized());
    assertEquals(0, likes.countByPhotoId(other));
  }
}
