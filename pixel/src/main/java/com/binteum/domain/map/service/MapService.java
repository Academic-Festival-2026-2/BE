package com.binteum.domain.map.service;

import com.binteum.domain.colorcell.enums.TimeBand;
import com.binteum.domain.map.dto.BoundaryResponse;
import com.binteum.domain.map.dto.MapDongsResponse;

public interface MapService {

  BoundaryResponse getBoundaries();

  MapDongsResponse getDongs(TimeBand timeBand);
}
