package com.pixel.domain.photo;

import static org.assertj.core.api.Assertions.assertThat;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.photo.enums.AiVerdict;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:mymap;MODE=MySQL;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1",
    "spring.test.database.replace=none"
})
@ActiveProfiles("test")
public class MyMapPhotoQueryTest {

  private static final LocalDate DAY = LocalDate.of(2026, 10, 29);

  @Autowired
  private EntityManager em;

  @Autowired
  private PhotoRepository photoRepository;

  @Test
  void includesManuallyEnteredPhotos() {
    Dong dong = dong("31050110");
    Season season = season();
    User me = user("me");
    User other = user("other");

    Photo exifPhoto = photoRepository.save(photo(me, dong, season, true));
    Photo manualPhoto = photoRepository.save(photo(me, dong, season, false));
    photoRepository.save(photo(other, dong, season, true));
    em.flush();
    em.clear();

    List<Photo> result = photoRepository.findMyMapPhotos(me.getUserId(), season);

    assertThat(result)
        .extracting(Photo::getPhotoId)
        .containsExactlyInAnyOrder(exifPhoto.getPhotoId(), manualPhoto.getPhotoId());
  }

  private Dong dong(String dongCode) {
    Dong dong = BeanUtils.instantiateClass(Dong.class);
    ReflectionTestUtils.setField(dong, "dongCode", dongCode);
    ReflectionTestUtils.setField(dong, "name", "테스트동");
    ReflectionTestUtils.setField(dong, "guName", "원미구");
    ReflectionTestUtils.setField(dong, "boundaryVersion", "20260101");
    em.persist(dong);
    return dong;
  }

  private Season season() {
    Season season = BeanUtils.instantiateClass(Season.class);
    ReflectionTestUtils.setField(season, "year", (short) 2026);
    ReflectionTestUtils.setField(season, "season", SeasonType.AUTUMN);
    ReflectionTestUtils.setField(season, "startsOn", DAY.minusMonths(1));
    ReflectionTestUtils.setField(season, "endsOn", DAY.plusMonths(1));
    em.persist(season);
    return season;
  }

  private User user(String name) {
    User user = User.builder().email(name + "@test.invalid").passwordHash("test")
        .nickname(name).termsAgreedAt(LocalDateTime.now())
        .privacyAgreedAt(LocalDateTime.now()).build();
    em.persist(user);
    return user;
  }

  private Photo photo(User owner, Dong dong, Season season, boolean exifVerified) {
    List<Double> bins = new ArrayList<>(Collections.nCopies(40, 0.0));
    bins.set(0, 1.0);
    return new Photo(owner, dong, season, TimeBand.DAY, DAY.atTime(13, 0), DAY, exifVerified,
        AiVerdict.PUBLIC, Collections.nCopies(5, "#123ABC"), bins, "original", "thumbnail",
        UUID.randomUUID().toString(), false);
  }
}
