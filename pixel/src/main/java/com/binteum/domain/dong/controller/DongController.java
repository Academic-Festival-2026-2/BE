package com.binteum.domain.dong.controller;

import com.binteum.domain.colorcell.enums.TimeBand;
import com.binteum.domain.dong.dto.DongDetailResponse;
import com.binteum.domain.dong.exception.code.DongSuccessCode;
import com.binteum.domain.dong.service.DongService;
import com.binteum.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dongs")
@RequiredArgsConstructor
public class DongController {

  private final DongService dongService;

  @GetMapping("/{dongCode}")
  public ResponseEntity<ApiResponse<DongDetailResponse>> getDetail(@PathVariable String dongCode,
      @RequestParam(required = false) TimeBand timeBand) {
    return ResponseEntity.ok(
        ApiResponse.of(DongSuccessCode._DETAIL_FOUND, dongService.getDetail(dongCode, timeBand)));
  }
}
