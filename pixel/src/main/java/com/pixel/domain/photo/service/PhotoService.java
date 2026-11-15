package com.pixel.domain.photo.service;

import com.pixel.domain.photo.dto.PhotoUploadRequest;
import com.pixel.domain.photo.dto.PhotoUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface PhotoService {

  PhotoUploadResponse upload(
      Long userId,
      MultipartFile file,
      PhotoUploadRequest request
  );
}
