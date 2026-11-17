package com.pixel.domain.colorcell;

import static org.assertj.core.api.Assertions.assertThat;

import com.pixel.domain.colorcell.entity.ColorCell;
import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.region.entity.Region;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:coloredcell;MODE=MySQL;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1",
    "spring.test.database.replace=none"
})
@ActiveProfiles("test")
class ColoredCellQueryTest {

  @Autowired
  private EntityManager em;

  @Autowired
  private ColorCellRepository colorCellRepository;

  // 나의 지도 미리보기 색은 요청한 지역에서 색이 정해진 칸만 읽는다.
  @Test
  void readsOnlyColoredCellsInRegion() {
    Season season = season();
    Dong bucheon = dong("31050110", region("3105"));
    Dong other = dong("31240110", region("3124"));

    ColorCell colored = cell(bucheon, season, TimeBand.DAY, "#123ABC");
    cell(bucheon, season, TimeBand.NIGHT, null);
    cell(other, season, TimeBand.DAY, "#123ABC");
    em.flush();
    em.clear();

    List<ColorCell> result = colorCellRepository.findColoredInRegion(season, "3105");

    assertThat(result)
        .extracting(ColorCell::getCellId)
        .containsExactly(colored.getCellId());
  }

  private Region region(String regionCode) {
    Region region = BeanUtils.instantiateClass(Region.class);
    ReflectionTestUtils.setField(region, "regionCode", regionCode);
    ReflectionTestUtils.setField(region, "sidoName", "경기도");
    ReflectionTestUtils.setField(region, "name", "테스트시");
    em.persist(region);
    return region;
  }

  private Dong dong(String dongCode, Region region) {
    Dong dong = BeanUtils.instantiateClass(Dong.class);
    ReflectionTestUtils.setField(dong, "dongCode", dongCode);
    ReflectionTestUtils.setField(dong, "name", "테스트동");
    ReflectionTestUtils.setField(dong, "guName", "테스트구");
    ReflectionTestUtils.setField(dong, "boundaryVersion", "20260101");
    ReflectionTestUtils.setField(dong, "region", region);
    em.persist(dong);
    return dong;
  }

  private Season season() {
    Season season = BeanUtils.instantiateClass(Season.class);
    ReflectionTestUtils.setField(season, "season", SeasonType.AUTUMN);
    em.persist(season);
    return season;
  }

  private ColorCell cell(Dong dong, Season season, TimeBand timeBand, String dominantColor) {
    ColorCell cell = new ColorCell(dong, season, timeBand);
    ReflectionTestUtils.setField(cell, "dominantColor", dominantColor);
    em.persist(cell);
    return cell;
  }
}
