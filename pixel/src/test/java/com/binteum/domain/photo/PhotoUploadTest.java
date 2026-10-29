package com.binteum.domain.photo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.binteum.domain.dong.entity.Dong;
import com.binteum.domain.dong.repository.DongRepository;
import com.binteum.domain.photo.controller.PhotoController;
import com.binteum.domain.photo.dto.PhotoUploadRequest;
import com.binteum.domain.photo.entity.Photo;
import com.binteum.domain.photo.repository.PhotoRepository;
import com.binteum.domain.photo.service.PhotoService;
import com.binteum.domain.photo.util.PhotoImageProcessor;
import com.binteum.domain.photo.util.PhotoMetadataReader;
import com.binteum.domain.season.entity.Season;
import com.binteum.domain.season.repository.SeasonRepository;
import com.binteum.domain.user.entity.User;
import com.binteum.domain.user.repository.UserRepository;
import com.binteum.global.apiPayload.exception.GlobalExceptionHandler;
import com.binteum.global.storage.S3StorageService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

class PhotoUploadTest {
  private final PhotoRepository photos = mock(PhotoRepository.class);
  private final UserRepository users = mock(UserRepository.class);
  private final DongRepository dongs = mock(DongRepository.class);
  private final SeasonRepository seasons = mock(SeasonRepository.class);
  private final PhotoMetadataReader metadata = mock(PhotoMetadataReader.class);
  private final PhotoImageProcessor images = mock(PhotoImageProcessor.class);
  private final S3StorageService storage = mock(S3StorageService.class);
  private final PhotoService service =
      new PhotoService(photos, users, dongs, seasons, metadata, images, storage);
  private final MockMultipartFile file =
      new MockMultipartFile(
          "file", "test.jpg", "image/jpeg", new byte[] {(byte) 255, (byte) 216, (byte) 255, 1});

  @BeforeEach
  void setup() {
    when(users.findById(1L)).thenReturn(Optional.of(mock(User.class)));
    when(dongs.findById("31050110")).thenReturn(Optional.of(mock(Dong.class)));
    when(seasons.findCurrent(any())).thenReturn(Optional.of(mock(Season.class)));
    when(metadata.read(any())).thenReturn(new PhotoMetadataReader.PhotoMetadata(null, null, null));
    when(images.process(any())).thenReturn(new PhotoImageProcessor.ImageData(new byte[] {1}));
    when(photos.saveAndFlush(any()))
        .thenAnswer(
            invocation -> {
              Photo photo = invocation.getArgument(0);
              ReflectionTestUtils.setField(photo, "photoId", 7L);
              return photo;
            });
    TransactionSynchronizationManager.initSynchronization();
  }

  @AfterEach
  void cleanup() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  private PhotoUploadRequest request(String time, Double latitude, Double longitude) {
    return new PhotoUploadRequest(
        "31050110",
        latitude,
        longitude,
        LocalDateTime.parse("2026-10-29T" + time),
        null,
        true,
        Collections.nCopies(5, "#123ABC"),
        Collections.nCopies(40, 0.025));
  }

  @ParameterizedTest
  @CsvSource({
    "04:59,NIGHT",
    "05:00,DAWN",
    "07:59,DAWN",
    "08:00,DAY",
    "15:59,DAY",
    "16:00,SUNSET",
    "18:59,SUNSET",
    "19:00,NIGHT"
  })
  void timeBandBoundaries(String time, String band) {
    var response = service.upload(1L, file, request(time, null, null));
    assertEquals(band, response.timeBand());
    assertFalse(response.exifVerified());
    verify(photos, never()).updateLocationAndVerification(any(), any(), any(), anyBoolean());
    complete(TransactionSynchronization.STATUS_COMMITTED);
    verify(storage, never()).deleteAllQuietly(any());
  }

  @Test
  void partialManualGpsCannotMixWithExif() {
    when(metadata.read(any()))
        .thenReturn(
            new PhotoMetadataReader.PhotoMetadata(
                LocalDateTime.parse("2026-10-29T13:00"), 37.5, 126.7));
    var ex =
        assertThrows(
            ResponseStatusException.class,
            () -> service.upload(1L, file, request("13:00", 37.6, null)));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    verifyNoInteractions(storage);
  }

  @Test
  void verifiedExifUpdatesCoordinatesAfterInsert() {
    when(metadata.read(any()))
        .thenReturn(
            new PhotoMetadataReader.PhotoMetadata(
                LocalDateTime.parse("2026-10-29T13:00"), 37.5, 126.7));
    when(dongs.findDongCodeByCoordinates(37.5, 126.7)).thenReturn(Optional.of("31050110"));
    when(photos.updateLocationAndVerification(7L, 37.5, 126.7, true)).thenReturn(1);
    assertTrue(service.upload(1L, file, request("13:00", null, null)).exifVerified());
    var order = inOrder(photos);
    order.verify(photos).saveAndFlush(any());
    order.verify(photos).updateLocationAndVerification(7L, 37.5, 126.7, true);
  }

  @Test
  void duplicateRejectedBeforeS3() {
    when(photos.existsByImageHash(anyString())).thenReturn(true);
    var ex =
        assertThrows(
            ResponseStatusException.class,
            () -> service.upload(1L, file, request("13:00", null, null)));
    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    verifyNoInteractions(storage);
  }

  @Test
  void damagedImageIsBadRequest() {
    when(images.process(any())).thenThrow(new IllegalArgumentException("broken"));
    var ex =
        assertThrows(
            ResponseStatusException.class,
            () -> service.upload(1L, file, request("13:00", null, null)));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    verifyNoInteractions(storage);
  }

  @Test
  void secondUploadFailureCleansOriginal() {
    doThrow(new IllegalStateException("S3 failure"))
        .when(storage)
        .upload(startsWith("photos/thumbnail/"), any(), anyString());
    assertThrows(
        IllegalStateException.class, () -> service.upload(1L, file, request("13:00", null, null)));
    verify(storage)
        .deleteAllQuietly(
            argThat(keys -> keys.size() == 1 && keys.get(0).startsWith("photos/original/")));
    verify(photos, never()).saveAndFlush(any());
  }

  @Test
  void databaseFailureRegistersRollbackCleanup() {
    doThrow(new DataIntegrityViolationException("constraint")).when(photos).saveAndFlush(any());
    assertThrows(
        DataIntegrityViolationException.class,
        () -> service.upload(1L, file, request("13:00", null, null)));
    complete(TransactionSynchronization.STATUS_ROLLED_BACK);
    verify(storage).deleteAllQuietly(argThat(keys -> keys.size() == 2));
  }

  private void complete(int status) {
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(sync -> sync.afterCompletion(status));
  }

  @ParameterizedTest
  @CsvSource({"400", "409"})
  void uploadStatusSurvivesGlobalExceptionHandler(int statusCode) throws Exception {
    PhotoService failing = mock(PhotoService.class);
    when(failing.upload(anyLong(), any(), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.valueOf(statusCode), "invalid"));
    var mvc =
        MockMvcBuilders.standaloneSetup(new PhotoController(failing))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    mvc.perform(
            multipart("/api/photos")
                .file(file)
                .file(new MockMultipartFile("metadata", "", "application/json", "{}".getBytes()))
                .principal(new UsernamePasswordAuthenticationToken(1L, null, List.of())))
        .andExpect(status().is(statusCode));
  }

  @Test
  void oversizedFileRejectedBeforeReading() {
    var oversized = mock(org.springframework.web.multipart.MultipartFile.class);
    when(oversized.getSize()).thenReturn(10L * 1024 * 1024 + 1);
    var ex =
        assertThrows(
            ResponseStatusException.class,
            () -> service.upload(1L, oversized, request("13:00", null, null)));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    verifyNoInteractions(storage, photos);
  }

  @Test
  void invalidPngSignatureRejected() {
    var invalid =
        new MockMultipartFile(
            "file",
            "fake.png",
            "image/png",
            new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0});
    assertThrows(
        ResponseStatusException.class,
        () -> service.upload(1L, invalid, request("13:00", null, null)));
    verifyNoInteractions(storage);
  }

  @Test
  void missingMetadataReturns400() throws Exception {
    var mvc =
        MockMvcBuilders.standaloneSetup(new PhotoController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    mvc.perform(multipart("/api/photos").file(file)).andExpect(status().isBadRequest());
  }

  @Test
  void realPngCreatesJpegThumbnail() throws Exception {
    var source =
        new java.awt.image.BufferedImage(1200, 600, java.awt.image.BufferedImage.TYPE_INT_ARGB);
    var output = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(source, "png", output);
    byte[] thumbnail = new PhotoImageProcessor().process(output.toByteArray()).thumbnailBytes();
    var decoded = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(thumbnail));
    assertEquals(600, decoded.getWidth());
    assertEquals(300, decoded.getHeight());
    assertEquals(0xFF, thumbnail[0] & 0xFF);
    assertEquals(0xD8, thumbnail[1] & 0xFF);
    assertNull(new PhotoMetadataReader().read(output.toByteArray()).takenAt());
  }
}
