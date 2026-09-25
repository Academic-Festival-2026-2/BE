package com.binteum.domain.user.entity;

import com.binteum.domain.user.enums.UserStatus;
import com.binteum.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Long userId;

  @Column(name = "student_id", length = 20, nullable = false, unique = true)
  private String studentId;

  @Column(name = "name", length = 30, nullable = false)
  private String name;

  @Column(name = "nickname", length = 30, nullable = false)
  private String nickname;

  @Column(name = "email", length = 100, nullable = false, unique = true)
  private String email;

  @Column(name = "password", length = 255, nullable = false)
  private String password;

  @Column(name = "department", length = 50)
  private String department;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 10, nullable = false)
  private UserStatus status = UserStatus.ACTIVE;
}




