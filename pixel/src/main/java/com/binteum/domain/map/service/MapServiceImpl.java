package com.binteum.domain.map.service;

import com.binteum.domain.dong.repository.DongRepository;
import com.binteum.domain.map.dto.BoundaryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapServiceImpl implements MapService {

  private final DongRepository dongRepository;

  @Override
  public BoundaryResponse getBoundaries() {
    List<BoundaryResponse.Feature> features =
        dongRepository.findAllBoundaries().stream().map(view -> new BoundaryResponse.Feature(
            new BoundaryResponse.Properties(view.getDongCode(), view.getName(), view.getGuName()),
            view.getGeometry())).toList();

    return new BoundaryResponse(dongRepository.findBoundaryVersion(), features);

  }
}
