package com.binteum.domain.study.dto;

import com.binteum.domain.study.enums.StudyCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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


