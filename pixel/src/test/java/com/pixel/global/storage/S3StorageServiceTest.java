package com.pixel.global.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class S3StorageServiceTest {

  @Test
  void signsAnEntirePhotoPageAndDoesNotCloseOtherServicesCredentials() {
    String accessKey = System.getProperty("aws.accessKeyId");
    String secretKey = System.getProperty("aws.secretAccessKey");
    System.setProperty("aws.accessKeyId", "test-access-key");
    System.setProperty("aws.secretAccessKey", "test-secret-key");
    S3StorageService first = null;
    S3StorageService second = null;
    try {
      first = new S3StorageService("ap-northeast-2", "test-bucket");
      second = new S3StorageService("ap-northeast-2", "test-bucket");
      first.readUrl("original.jpg");
      first.close();
      first = null;
      for (int i = 0; i < 100; i++) {
        URI url = URI.create(second.readUrl("thumbnail-" + i + ".jpg"));
        assertEquals("https", url.getScheme());
        assertEquals("test-bucket.s3.ap-northeast-2.amazonaws.com", url.getHost());
        assertEquals("/thumbnail-" + i + ".jpg", url.getPath());
        assertTrue(url.getQuery().contains("X-Amz-Expires=300"));
        assertTrue(url.getQuery().contains("X-Amz-Signature="));
      }
    } finally {
      if (first != null) {
        first.close();
      }
      if (second != null) {
        second.close();
      }
      restoreProperty("aws.accessKeyId", accessKey);
      restoreProperty("aws.secretAccessKey", secretKey);
    }
  }

  private static void restoreProperty(String name, String value) {
    if (value == null) {
      System.clearProperty(name);
    } else {
      System.setProperty(name, value);
    }
  }
}
