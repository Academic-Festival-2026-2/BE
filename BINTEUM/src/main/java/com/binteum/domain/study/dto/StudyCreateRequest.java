package com.binteum.domain.study.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudyCreateRequest {

  @NotBlank
  private String title;

  @NotBlank
  private String category;

  @NotNull
  private Integer maxParticipant;

  @NotNull
  private LocalDateTime startTime;

  @NotNull
  private LocalDateTime endTime;
}


