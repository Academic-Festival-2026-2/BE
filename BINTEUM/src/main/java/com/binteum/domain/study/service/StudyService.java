package com.binteum.domain.study.service;

import com.binteum.domain.study.dto.StudyCreateRequest;
import com.binteum.domain.study.dto.StudyResponse;

public interface StudyService {

  StudyResponse createStudy(Long userId, StudyCreateRequest request);

}


