package com.binteum.domain.study.dto;

import com.binteum.domain.study.enums.StudyCategory;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class StudyCreateRequest {

  @NotNull
  private Long roomId;

  @NotBlank
  @Size(max = 100)
  private String title;

  @Size(max = 500)
  private String description;

  @NotNull
  private StudyCategory category;

  @NotNull
  @Min(2)
  private Integer maxParticipant;

  @NotNull
  @Future
  private LocalDateTime startTime;

  @NotNull
  private LocalDateTime endTime;
}


