package com.pixel.domain.photo.dto;

import jakarta.validation.constraints.NotNull;

public record PhotoVisibilityRequest(@NotNull Boolean isPublic) {}
