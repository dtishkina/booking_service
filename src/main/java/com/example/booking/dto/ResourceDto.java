package com.example.booking.dto;

import com.example.booking.entity.ResourceType;

public record ResourceDto(
        Long id,
        String name,
        ResourceType type,
        Integer capacity,
        String location,
        boolean active
) {}
