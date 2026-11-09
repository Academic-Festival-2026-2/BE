package com.pixel.domain.mymap.dto;

import com.pixel.domain.dong.dto.DongDetailResponse;
import java.util.List;

public record MyDongPhotosResponse(String dongCode, List<DongDetailResponse.Point> points) {}
