package com.pixel.global.storage;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Service
public class S3StorageService {

  private final S3Client s3Client;
  private final String bucket;
  private final S3Presigner presigner;
  private final DefaultCredentialsProvider credentialsProvider;

  public S3StorageService(
      @Value("${AWS_REGION:ap-northeast-2}") String region,
      @Value("${AWS_S3_BUCKET:}") String bucket) {
    this.credentialsProvider = DefaultCredentialsProvider.builder().build();
    this.s3Client =
        S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(credentialsProvider)
            .build();
    this.bucket = bucket;
    this.presigner = S3Presigner.builder()
        .region(Region.of(region))
        .credentialsProvider(credentialsProvider)
        .build();
  }

  // 호출자가 조회 권한을 확인한 뒤, 비공개 버킷의 짧게 유효한 조회 URL을 발급한다.
  public String readUrl(String key) {
    checkBucket();
    return presigner.presignGetObject(request -> request.signatureDuration(Duration.ofMinutes(5))
        .getObjectRequest(object -> object.bucket(bucket).key(key))).url().toString();
  }

  @PreDestroy
  public void close() {
    try {
      presigner.close();
    } finally {
      try {
        s3Client.close();
      } finally {
        credentialsProvider.close();
      }
    }
  }

  public void upload(String key, byte[] bytes, String contentType) {
    checkBucket();

    PutObjectRequest request =
        PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build();

    s3Client.putObject(request, RequestBody.fromBytes(bytes));
  }

  public void delete(String key) {
    checkBucket();

    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
  }

  public void deleteQuietly(String key) {
    if (key == null) {
      return;
    }

    try {
      delete(key);
    } catch (Exception ignored) {
      // 정리 실패가 원래 예외를 덮지 않도록 처리
    }
  }

  public void deleteAllQuietly(List<String> keys) {
    for (String key : keys) {
      deleteQuietly(key);
    }
  }

  private void checkBucket() {
    if (bucket == null || bucket.isBlank()) {
      throw new IllegalStateException("AWS_S3_BUCKET 환경변수가 설정되지 않았습니다.");
    }
  }
}
