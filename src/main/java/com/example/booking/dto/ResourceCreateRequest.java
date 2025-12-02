package com.example.booking.dto;

import com.example.booking.entity.ResourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResourceCreateRequest(
        @NotBlank
        String name,

        @NotNull
        ResourceType type,

        @Min(1)
        Integer capacity,

        String location
) {}