package com.binteum.domain.study.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.binteum.domain.study.enums.StudyCategory;
import com.binteum.domain.study.enums.StudyDisplayStatus;
import com.binteum.domain.study.enums.StudyStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class StudyTest {

  //기준시각 : 10월 12일 12:00
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 12, 12, 0);

  //14:00-16:00, 정원 4인 스터디
  private Study createStudy() {
    return Study.builder()
        .title("테스트 스터디")
        .category(StudyCategory.MAJOR)
        .maxParticipant(4)
        .startTime(LocalDateTime.of(2026, 10, 12, 14, 0))
        .endTime(LocalDateTime.of(2026, 10, 12, 16, 0))
        .build();
  }

  @Test
  void before_start_recruit_if_empty() {
    Study study = createStudy();

    assertThat(study.calculateDisplayStatus(1, NOW))
        .isEqualTo(StudyDisplayStatus.RECRUITING);
  }

  @Test
  void before_start_full_if_no_seats_left() {
    Study study = createStudy();

    assertThat(study.calculateDisplayStatus(4, NOW))
        .isEqualTo(StudyDisplayStatus.FULL);
  }

  @Test
  void in_progress_after_start() {
    Study study = createStudy();

    assertThat(study.calculateDisplayStatus(1, LocalDateTime.of(2026, 10, 12, 15, 0))).isEqualTo(
        StudyDisplayStatus.IN_PROGRESS);
  }

  @Test
  void completed_at_end_time() {
    Study study = createStudy();

    assertThat(study.calculateDisplayStatus(1, LocalDateTime.of(2026, 10, 12, 16, 0))).isEqualTo(
        StudyDisplayStatus.COMPLETED);
  }

  @Test
  void cancelled_even_if_full() {
    Study study = createStudy();
    study.setStatus(StudyStatus.CANCELLED);

    assertThat(study.calculateDisplayStatus(4, NOW)).isEqualTo(StudyDisplayStatus.CANCELLED);
  }
}
