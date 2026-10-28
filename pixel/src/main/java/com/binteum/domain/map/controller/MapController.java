package com.binteum.domain.map.controller;

import com.binteum.domain.colorcell.enums.TimeBand;
import com.binteum.domain.map.dto.BoundaryResponse;
import com.binteum.domain.map.dto.MapDongsResponse;
import com.binteum.domain.map.exception.code.MapSuccessCode;
import com.binteum.domain.map.service.MapService;
import com.binteum.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

  private final MapService mapService;

  @GetMapping("/boundaries")
  public ResponseEntity<ApiResponse<BoundaryResponse>> getBoundaries() {
    return ResponseEntity.ok(
        ApiResponse.of(MapSuccessCode._BOUNDARIES_FOUND, mapService.getBoundaries()));
  }

  @GetMapping("/dongs")
  public ResponseEntity<ApiResponse<MapDongsResponse>> getDongs(@RequestParam TimeBand timeBand) {
    return ResponseEntity.ok(
        ApiResponse.of(MapSuccessCode._MAP_FOUND, mapService.getDongs(timeBand)));
  }
}
