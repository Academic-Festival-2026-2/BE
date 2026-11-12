package com.pixel.domain.mymap.controller;

import com.pixel.domain.mymap.dto.SharedMapResponse;
import com.pixel.domain.mymap.exception.code.MyMapSuccessCode;
import com.pixel.domain.mymap.service.MyMapService;
import com.pixel.domain.region.entity.Region;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shared-maps")
@RequiredArgsConstructor
public class SharedMapController {

  private final MyMapService myMapService;


  @GetMapping("/{shareKey}")
  public ResponseEntity<ApiResponse<SharedMapResponse>> getSharedMap(
      @PathVariable String shareKey,
      @RequestParam(defaultValue = "AUTUMN") SeasonType season,
      @RequestParam(defaultValue = Region.DEFAULT_CODE) String regionCode) {
    return ResponseEntity.ok(ApiResponse.of(MyMapSuccessCode._SHARED_MAP_FOUND,
        myMapService.getSharedMap(shareKey, season, regionCode)));
  }
}
