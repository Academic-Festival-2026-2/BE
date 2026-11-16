package com.pixel.domain.season;

import static org.assertj.core.api.Assertions.assertThat;

import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.season.repository.SeasonRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SeasonBoundaryRepositoryTest {
  @Autowired private SeasonRepository repository;
  @Autowired private TestEntityManager em;

  @Test
  void 연도가_달라도_같은_계절을_찾고_경계일을_지킨다() {
    for (SeasonType type : SeasonType.values()) {
      Season season = BeanUtils.instantiateClass(Season.class);
      ReflectionTestUtils.setField(season, "season", type);
      em.persistAndFlush(season);
    }
    assertThat(repository.findCurrent(LocalDate.of(2024, 3, 1)).orElseThrow().getSeason())
        .isEqualTo(SeasonType.SPRING);
    assertThat(repository.findCurrent(LocalDate.of(2026, 5, 31)).orElseThrow().getSeason())
        .isEqualTo(SeasonType.SPRING);
    assertThat(repository.findCurrent(LocalDate.of(2025, 6, 1)).orElseThrow().getSeason())
        .isEqualTo(SeasonType.SUMMER);
    assertThat(repository.findCurrent(LocalDate.of(2026, 11, 30)).orElseThrow().getSeason())
        .isEqualTo(SeasonType.AUTUMN);
    assertThat(repository.findCurrent(LocalDate.of(2026, 12, 1)).orElseThrow().getSeason())
        .isEqualTo(SeasonType.WINTER);
    assertThat(repository.findCurrent(LocalDate.of(2028, 2, 29)).orElseThrow().getSeason())
        .isEqualTo(SeasonType.WINTER);
    assertThat(repository.findCurrent(LocalDate.of(2030, 3, 1)).orElseThrow().getSeason())
        .isEqualTo(SeasonType.SPRING);
    assertThat(repository.findBySeason(SeasonType.SPRING)).isPresent();
  }
}
