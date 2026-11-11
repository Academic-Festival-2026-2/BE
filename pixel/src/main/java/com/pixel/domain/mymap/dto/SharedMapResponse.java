package com.pixel.domain.mymap.dto;

import com.pixel.domain.user.enums.ProfileImage;

public record SharedMapResponse(String nickname, ProfileImage profileImage, MyMapResponse map) {}
