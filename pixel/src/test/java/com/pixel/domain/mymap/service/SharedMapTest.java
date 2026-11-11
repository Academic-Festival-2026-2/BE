package com.pixel.domain.mymap.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.dong.entity.Dong;
import com.pixel.domain.dong.repository.DongRepository;
import com.pixel.domain.mymap.dto.MyMapResponse;
import com.pixel.domain.mymap.dto.SharedMapResponse;
import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.photo.repository.PhotoRepository;
import com.pixel.domain.season.entity.Season;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.domain.season.service.SeasonService;
import com.pixel.domain.user.entity.User;
import com.pixel.domain.user.enums.ProfileImage;
import com.pixel.domain.user.enums.UserStatus;
import com.pixel.domain.user.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class SharedMapTest {

  private static final Long OWNER_ID = 1L;
  private static final String SHARE_KEY = "3f9a1c0e7b2d4e8fa61c5d0b9e2f7a14";

  @Mock
  private SeasonService seasonService;

  @Mock
  private DongRepository dongRepository;

  @Mock
  private PhotoRepository photoRepository;

  @Mock
  private ColorCellRepository colorCellRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private MyMapServiceImpl myMapService;

  @Test
  void excludesPrivatePhotosFromSharedMap() {
    User owner = mock(User.class);
    given(owner.getUserId()).willReturn(OWNER_ID);
    given(owner.getNickname()).willReturn("노을러");
    given(owner.getProfileImage()).willReturn(ProfileImage.CAT_CHEESE);
    given(userRepository.findByShareKeyAndStatus(SHARE_KEY, UserStatus.ACTIVE))
        .willReturn(Optional.of(owner));

    Season season = mock(Season.class);
    Dong dong = dong("31051581");
    // mock 생성이 다른 설정 도중에 일어나지 않도록 목록을 먼저 만든다
    List<Photo> photos = List.of(publicPhoto(dong), privatePhoto());
    given(seasonService.getSeason(SeasonType.AUTUMN)).willReturn(season);
    given(dongRepository.findAll(Sort.by("dongCode"))).willReturn(List.of(dong));
    given(photoRepository.findMyMapPhotos(OWNER_ID, season)).willReturn(photos);

    SharedMapResponse response = myMapService.getSharedMap(SHARE_KEY, SeasonType.AUTUMN);

    assertThat(response.nickname()).isEqualTo("노을러");
    assertThat(response.profileImage()).isEqualTo(ProfileImage.CAT_CHEESE);
    MyMapResponse.Cell sunset = response.map().getCells()
        .stream()
        .filter(cell -> cell.getTimeBand() == TimeBand.SUNSET)
        .findFirst()
        .orElseThrow();
    assertThat(sunset.getMyPhotoCount()).isEqualTo(1);
  }

  private Dong dong(String dongCode) {
    Dong dong = mock(Dong.class);
    given(dong.getDongCode()).willReturn(dongCode);
    given(dong.getName()).willReturn(dongCode + "동");
    return dong;
  }

  private Photo publicPhoto(Dong dong) {
    Photo photo = mock(Photo.class);
    given(photo.isPublic()).willReturn(true);
    given(photo.getDong()).willReturn(dong);
    given(photo.getTimeBand()).willReturn(TimeBand.SUNSET);
    given(photo.getBins()).willReturn(Collections.nCopies(40, 1.0));
    given(photo.getWeight()).willReturn(1.0f);
    return photo;
  }

  // 비공개 사진은 filter에서 바로 걸러져서 isPublic() 말고는 불리지 않는다
  private Photo privatePhoto() {
    Photo photo = mock(Photo.class);
    given(photo.isPublic()).willReturn(false);
    return photo;
  }
}
