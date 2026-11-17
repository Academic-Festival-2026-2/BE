package com.pixel.domain.map.service;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.map.dto.BoundaryResponse;
import com.pixel.domain.map.dto.MapDongsResponse;
import com.pixel.domain.map.dto.RegionListResponse;
import com.pixel.domain.season.enums.SeasonType;

public interface MapService {

  BoundaryResponse getBoundaries(String regionCode);

  MapDongsResponse getDongs(TimeBand timeBand, String regionCode, SeasonType season);

  RegionListResponse getRegions();
}
