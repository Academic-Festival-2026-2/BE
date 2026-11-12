package com.pixel.domain.region.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "regions")
public class Region {

  // 지역 코드를 안보내면 부천, FE에서 지역 고를 수 있게 되면 삭제하겠음.
  public static final String DEFAULT_CODE = "3105";

  @Id
  @Column(name = "region_code", columnDefinition = "CHAR(4)")
  private String regionCode;

  @Column(name = "sido_name", length = 10, nullable = false)
  private String sidoName;

  @Column(name = "name", length = 10, nullable = false)
  private String name;
}
