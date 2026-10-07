package com.binteum.domain.study.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudyListResponse {

  private List<StudySummaryResponse> studies;
  private Integer page;
  private Integer size;
  private Long totalElements;
  private Integer totalPages;
  private Boolean hasNext;

}
