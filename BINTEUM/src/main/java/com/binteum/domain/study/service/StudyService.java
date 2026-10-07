package com.binteum.domain.study.service;

import com.binteum.domain.study.dto.StudyCreateRequest;
import com.binteum.domain.study.dto.StudyDetailResponse;
import com.binteum.domain.study.dto.StudyListResponse;
import com.binteum.domain.study.dto.StudyResponse;
import com.binteum.domain.study.enums.StudyCategory;
import com.binteum.domain.study.enums.StudyListTab;

public interface StudyService {

  StudyResponse createStudy(Long userId, StudyCreateRequest request);

  StudyDetailResponse getStudy(Long studyId, Long userId);

  StudyListResponse getStudies(StudyListTab tab, String keyword, Long buildingId,
      StudyCategory category, int page, int size);
}


