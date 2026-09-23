package com.binteum.domain.user.controller;

import com.binteum.domain.user.dto.UserProfileResponse;
import com.binteum.domain.user.enums.UserStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

  @GetMapping("/{userId}")
  public ResponseEntity<UserProfileResponse> getUser(@PathVariable Long userId) {
    return ResponseEntity.ok(new UserProfileResponse(
        userId,
        "20250001",
        "홍길동",
        "길동이",
        "hong@example.com",
        "컴퓨터공학과",
        UserStatus.ACTIVE
    ));
  }
}


