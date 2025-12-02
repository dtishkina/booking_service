package com.example.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record BookingCreateRequest(
        @NotNull
        Long resourceId,

        @NotNull
        OffsetDateTime startTime,

        @NotNull
        OffsetDateTime endTime
) {}