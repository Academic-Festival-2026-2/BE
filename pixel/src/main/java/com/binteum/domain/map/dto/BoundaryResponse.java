package com.binteum.domain.map.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BoundaryResponse {

  private final String version;
  private final String type = "FeatureCollection";
  private final List<Feature> features;

  @Getter
  @AllArgsConstructor
  public static class Feature {

    private final String type = "Feature";
    private final Properties properties;

    @JsonRawValue
    private final String geometry;
  }

  @Getter
  @AllArgsConstructor
  public static class Properties {

    private final String dongCode;
    private final String name;
    private final String guName;
  }
}
