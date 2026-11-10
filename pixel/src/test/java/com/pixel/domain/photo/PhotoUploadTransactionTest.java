package com.pixel.domain.photo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pixel.domain.colorcell.service.ColorCellService;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.photo.dto.PhotoUploadRequest;
import com.pixel.domain.photo.enums.AiReason;
import com.pixel.domain.photo.enums.AiVerdict;
import com.pixel.domain.photo.moderation.ImageModerator;
import com.pixel.domain.photo.moderation.ModerationResult;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.photo.service.PhotoService;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.user.entity.User;
import com.pixel.global.storage.S3StorageService;
import jakarta.persistence.EntityManager;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// 검수는 트랜잭션 밖, S3 업로드부터 저장·집계까지는 실제 Spring 트랜잭션 안에서 도는지 확인한다.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:upload-tx;MODE=MySQL;"
    + "NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class PhotoUploadTransactionTest {

  static final String DONG = "31050110";
  static final LocalDate DAY = LocalDate.of(2026, 10, 29);

  @Autowired PhotoService service;
  @Autowired PhotoRepository photos;
  @Autowired EntityManager em;
  @Autowired PlatformTransactionManager manager;
  @MockitoBean S3StorageService storage;
  @MockitoBean ImageModerator moderator;
  @MockitoBean ColorCellService cells;

  TransactionTemplate tx;
  Long userId;

  @BeforeEach
  void setup() {
    when(cells.aggregate(any())).thenReturn(new ColorCellService.Contribution(null, false));
    tx = new TransactionTemplate(manager);
    tx.executeWithoutResult(status -> {
      photos.deleteAllInBatch();
      em.createQuery("DELETE FROM User").executeUpdate();
      em.createQuery("DELETE FROM Season").executeUpdate();
      em.createQuery("DELETE FROM Dong").executeUpdate();
      var dong = BeanUtils.instantiateClass(Dong.class);
      ReflectionTestUtils.setField(dong, "dongCode", DONG);
      ReflectionTestUtils.setField(dong, "name", "심곡동");
      ReflectionTestUtils.setField(dong, "guName", "원미구");
      ReflectionTestUtils.setField(dong, "boundaryVersion", "20260101");
      em.persist(dong);
      var season = BeanUtils.instantiateClass(Season.class);
      ReflectionTestUtils.setField(season, "year", (short) 2026);
      ReflectionTestUtils.setField(season, "season", SeasonType.AUTUMN);
      ReflectionTestUtils.setField(season, "startsOn", DAY.minusMonths(1));
      ReflectionTestUtils.setField(season, "endsOn", DAY.plusMonths(1));
      em.persist(season);
      var user = User.builder().email("upload-tx@test.invalid").passwordHash("test")
          .nickname("uploadtx").termsAgreedAt(LocalDateTime.now())
          .privacyAgreedAt(LocalDateTime.now()).build();
      em.persist(user);
      userId = user.getUserId();
    });
  }

  MockMultipartFile jpeg() throws Exception {
    var image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
    var out = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", out);
    return new MockMultipartFile("image", "photo.jpg", "image/jpeg", out.toByteArray());
  }

  PhotoUploadRequest request() {
    return new PhotoUploadRequest(DONG, null, null, DAY.atTime(13, 0), null, true,
        Collections.nCopies(5, "#123ABC"), Collections.nCopies(40, 0.025));
  }

  @Test
  void personalOnlyPhotoIsCommittedPrivateWithVerdict() throws Exception {
    when(moderator.moderate(any()))
        .thenReturn(ModerationResult.personalOnly(ModerationResult.Reason.PRIVACY));

    var response = service.upload(userId, jpeg(), request());

    assertEquals(AiVerdict.PERSONAL_ONLY, response.aiVerdict());
    assertEquals(AiReason.PRIVACY, response.aiReason());
    tx.executeWithoutResult(status -> {
      var saved = photos.findById(response.photoId()).orElseThrow();
      assertEquals(AiVerdict.PERSONAL_ONLY, saved.getAiVerdict());
      assertFalse(saved.isPublic());
    });
    verify(storage, times(2)).upload(anyString(), any(), anyString());
    verify(storage, never()).deleteAllQuietly(any());
  }

  @Test
  void failureAfterS3RollsBackPhotoAndCleansUploadedFiles() throws Exception {
    when(moderator.moderate(any())).thenReturn(ModerationResult.approved());
    when(cells.aggregate(any())).thenThrow(new IllegalStateException("aggregation failed"));

    assertThrows(IllegalStateException.class, () -> service.upload(userId, jpeg(), request()));

    assertEquals(0, photos.count());
    verify(storage).deleteAllQuietly(argThat(keys -> keys.size() == 2));
  }
}
