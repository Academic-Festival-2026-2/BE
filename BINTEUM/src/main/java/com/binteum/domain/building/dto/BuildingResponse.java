package com.binteum.domain.building.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BuildingResponse {
  private Long buildingId;
  private String name;
  private Double latitude;
  private Double longitude;
}


