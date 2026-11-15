package com.pixel.domain.season;

import static org.assertj.core.api.Assertions.assertThat;

import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.season.repository.SeasonRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SeasonBoundaryRepositoryTest {

  @Autowired
  private SeasonRepository repository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  void november30IsAutumnAndDecember1IsWinter() {
    save(SeasonType.AUTUMN, "2026-09-01", "2026-11-30");
    save(SeasonType.WINTER, "2026-12-01", "2027-02-28");

    assertThat(repository.findCurrent(LocalDate.of(2026, 11, 30)))
        .hasValueSatisfying(s ->
            assertThat(s.getSeason()).isEqualTo(SeasonType.AUTUMN));

    assertThat(repository.findCurrent(LocalDate.of(2026, 12, 1)))
        .hasValueSatisfying(s ->
            assertThat(s.getSeason()).isEqualTo(SeasonType.WINTER));

    assertThat(repository.findCurrent(LocalDate.of(2027, 3, 1))).isEmpty();
  }

  private void save(SeasonType type, String start, String end) {
    Season season = BeanUtils.instantiateClass(Season.class);
    ReflectionTestUtils.setField(season, "year", (short) 2026);
    ReflectionTestUtils.setField(season, "season", type);
    ReflectionTestUtils.setField(season, "startsOn", LocalDate.parse(start));
    ReflectionTestUtils.setField(season, "endsOn", LocalDate.parse(end));
    entityManager.persistAndFlush(season);
  }
}
