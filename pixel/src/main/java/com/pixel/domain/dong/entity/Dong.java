package com.pixel.domain.dong.entity;

import com.pixel.domain.region.entity.Region;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "dongs")
public class Dong {

  @Id
  @Column(name = "dong_code", columnDefinition = "CHAR(10)")
  private String dongCode;

  @Column(name = "name", length = 20, nullable = false)
  private String name;

  @Column(name = "gu_name", length = 10)
  private String guName;

  @Column(name = "boundary_version", length = 8, nullable = false)
  private String boundaryVersion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "region_code", columnDefinition = "CHAR(4)", nullable = false)
  private Region region;
}
