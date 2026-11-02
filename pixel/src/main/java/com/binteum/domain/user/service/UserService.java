package com.binteum.domain.user.service;

import com.binteum.domain.user.dto.ChangePasswordRequest;
import com.binteum.domain.user.dto.MyInfoResponse;
import com.binteum.domain.user.dto.UpdateMyInfoRequest;
import com.binteum.domain.user.dto.WithdrawRequest;

public interface UserService {

  MyInfoResponse getMyInfo(Long userId);

  void updateMyInfo(Long userId, UpdateMyInfoRequest request);

  void changePassword(Long userId, ChangePasswordRequest request);

  void withdraw(Long userId, WithdrawRequest request);
}
