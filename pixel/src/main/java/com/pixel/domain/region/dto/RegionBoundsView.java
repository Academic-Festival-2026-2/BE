package com.pixel.domain.region.dto;

public interface RegionBoundsView {

  String getRegionCode();

  String getName();

  int getDongCount();

  double getMinLng();
  double getMaxLng();
  double getMinLat();
  double getMaxLat();
}
