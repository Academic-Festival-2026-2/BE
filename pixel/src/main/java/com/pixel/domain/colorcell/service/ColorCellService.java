package com.pixel.domain.colorcell.service;

import com.pixel.domain.colorcell.entity.ColorCell;
import com.pixel.domain.colorcell.repository.ColorCellRepository;
import com.pixel.domain.dong.repository.DongRepository;
import com.pixel.domain.photo.entity.Photo;
import com.pixel.domain.photo.repository.PhotoRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ColorCellService {

  private final ColorCellRepository cells;
  private final DongRepository dongs;
  private final PhotoRepository photos;
  private final EntityManager entityManager;

  // 사진 INSERT의 FK 잠금보다 먼저 획득한다. 없는 셀의 생성도 동 단위로 직렬화한다.
  public void lockForUpload(String dongCode) {
    dongs.findForUpdate(dongCode)
        .orElseThrow(() -> new IllegalStateException("집계할 행정동이 존재하지 않습니다."));
  }

  // 호출자는 같은 트랜잭션에서 사진 저장 전에 lockForUpload를 호출해야 한다.
  public Contribution aggregate(Photo photo) {
    // 네이티브 GPS UPDATE 이후 영속성 컨텍스트의 검증 상태를 DB와 동기화한다.
    entityManager.refresh(photo);
    if (!photo.isExifVerified()) {
      return new Contribution(null, false);
    }
    ColorCell cell = cells.findForUpdate(photo.getDong(), photo.getSeason(), photo.getTimeBand())
        .orElseGet(() -> new ColorCell(photo.getDong(), photo.getSeason(), photo.getTimeBand()));
    boolean first = cell.getPhotoCount() == 0;
    boolean newShot = photos.findPreviousShot(photo.getUser().getUserId(), photo.getShotDate(),
        photo.getDong().getDongCode(), photo.getSeason().getSeasonId(), photo.getTimeBand(),
        photo.getPhotoId(), PageRequest.of(0, 1)).isEmpty();
    cell.contribute(photo.getPhotoId(), photo.getBins(), photo.getWeight(), newShot);
    cells.saveAndFlush(cell);
    return new Contribution(cell.getPhotoCount(), first);
  }

  public record Contribution(Integer cellPhotoCount, boolean firstInCell) {}
}
