package com.pixel.domain.map.controller;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.map.dto.BoundaryResponse;
import com.pixel.domain.map.dto.MapDongsResponse;
import com.pixel.domain.map.dto.RegionListResponse;
import com.pixel.domain.map.exception.code.MapSuccessCode;
import com.pixel.domain.map.service.MapService;
import com.pixel.domain.region.entity.Region;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.global.apiPayload.ApiResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

  // 경계·지역 목록은 데이터를 새로 넣을 때만 바뀌므로 브라우저가 1시간 동안 다시 받지 않게 한다.
  private static final CacheControl MAP_DATA_CACHE = CacheControl.maxAge(Duration.ofHours(1));

  private final MapService mapService;

  @GetMapping("/boundaries")
  public ResponseEntity<ApiResponse<BoundaryResponse>> getBoundaries(
      @RequestParam(defaultValue = Region.DEFAULT_CODE) String regionCode) {
    BoundaryResponse result = mapService.getBoundaries(regionCode);
    return ResponseEntity.ok().cacheControl(MAP_DATA_CACHE)
        .body(ApiResponse.of(MapSuccessCode._BOUNDARIES_FOUND, result));
  }

  @GetMapping("/dongs")
  public ResponseEntity<ApiResponse<MapDongsResponse>> getDongs(@RequestParam TimeBand timeBand,
      @RequestParam(defaultValue = Region.DEFAULT_CODE) String regionCode,
      @RequestParam(required = false) SeasonType season) {
    MapDongsResponse result = mapService.getDongs(timeBand, regionCode, season);
    return ResponseEntity.ok(ApiResponse.of(MapSuccessCode._MAP_FOUND, result));
  }

  @GetMapping("/regions")
  public ResponseEntity<ApiResponse<RegionListResponse>> getRegions() {
    RegionListResponse result = mapService.getRegions();
    return ResponseEntity.ok().cacheControl(MAP_DATA_CACHE)
        .body(ApiResponse.of(MapSuccessCode._REGIONS_FOUND, result));
  }
}
