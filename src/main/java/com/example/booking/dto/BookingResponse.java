package com.example.booking.dto;

import com.example.booking.entity.BookingStatus;

import java.time.OffsetDateTime;

public record BookingResponse(
        Long id,
        Long userId,
        Long resourceId,
        String resourceName,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        BookingStatus status
) {}
