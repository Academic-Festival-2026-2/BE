package com.binteum.domain.feedback.dto;

import com.binteum.domain.feedback.enums.FeedbackIssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FeedbackCreateRequest {

  @NotNull
  private FeedbackIssueType issueType;

  @NotBlank
  private String description;
}


