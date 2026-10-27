package com.binteum.domain.dong.entity;

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
@Table(name = "dongs")
public class Dong {

  @Id
  @Column(name = "dong_code", columnDefinition = "CHAR(10)")
  private String dongCode;

  @Column(name = "name", length = 20, nullable = false)
  private String name;

  @Column(name = "gu_name", length = 10, nullable = false)
  private String guName;

  @Column(name = "boundary_version", length = 8, nullable = false)
  private String boundaryVersion;
}
