package com.binteum.global.storage;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3StorageService {

  private final S3Client s3Client;
  private final String bucket;

  public S3StorageService(
      @Value("${AWS_REGION:ap-northeast-2}") String region,
      @Value("${AWS_S3_BUCKET:}") String bucket) {
    this.s3Client =
        S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    this.bucket = bucket;
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
