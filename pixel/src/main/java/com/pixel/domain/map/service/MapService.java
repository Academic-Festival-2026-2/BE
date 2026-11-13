package com.pixel.domain.map.service;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.map.dto.BoundaryResponse;
import com.pixel.domain.map.dto.MapDongsResponse;
import com.pixel.domain.map.dto.RegionListResponse;

public interface MapService {

  BoundaryResponse getBoundaries(String regionCode);

  MapDongsResponse getDongs(TimeBand timeBand, String regionCode);

  RegionListResponse getRegions();
}
