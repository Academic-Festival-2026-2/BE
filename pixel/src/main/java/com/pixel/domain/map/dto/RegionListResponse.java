package com.pixel.domain.map.dto;

import java.util.List;

public record RegionListResponse(String defaultRegionCode, List<Item> regions) {

  public record Item(String regionCode, String name, int dongCount, List<Double> bbox) {}
}
