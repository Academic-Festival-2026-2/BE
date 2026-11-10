package com.pixel.domain.mymap.controller;

import com.pixel.domain.colorcell.enums.TimeBand;
import com.pixel.domain.mymap.dto.MyDongPhotosResponse;
import com.pixel.domain.mymap.dto.MyMapResponse;
import com.pixel.domain.mymap.exception.code.MyMapSuccessCode;
import com.pixel.domain.mymap.service.MyMapService;
import com.pixel.domain.season.enums.SeasonType;
import com.pixel.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/map")
@RequiredArgsConstructor
public class MyMapController {

  private final MyMapService myMapService;

  @GetMapping
  public ResponseEntity<ApiResponse<MyMapResponse>> getMyMap(@AuthenticationPrincipal Long userId,
      @RequestParam(defaultValue = "AUTUMN") SeasonType season) {
    return ResponseEntity.ok(
        ApiResponse.of(MyMapSuccessCode._MY_MAP_FOUND, myMapService.getMyMap(userId, season)));
  }

  @GetMapping("/dongs/{dongCode}")
  public ResponseEntity<ApiResponse<MyDongPhotosResponse>> getMyDongPhotos(
      @AuthenticationPrincipal Long userId,
      @PathVariable String dongCode,
      @RequestParam(required = false) TimeBand timeBand,
      @RequestParam(defaultValue = "AUTUMN") SeasonType season) {
    return ResponseEntity.ok(ApiResponse.of(MyMapSuccessCode._MY_DONG_PHOTOS_FOUND,
        myMapService.getMyDongPhotos(userId, dongCode, timeBand, season)));
  }
}
