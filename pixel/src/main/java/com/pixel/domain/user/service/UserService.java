package com.pixel.domain.user.service;

import com.pixel.domain.user.dto.ChangePasswordRequest;
import com.pixel.domain.user.dto.MyInfoResponse;
import com.pixel.domain.user.dto.UpdateMyInfoRequest;
import com.pixel.domain.user.dto.WithdrawRequest;

public interface UserService {

  MyInfoResponse getMyInfo(Long userId);

  void updateMyInfo(Long userId, UpdateMyInfoRequest request);

  void changePassword(Long userId, ChangePasswordRequest request);

  void withdraw(Long userId, WithdrawRequest request);
}
