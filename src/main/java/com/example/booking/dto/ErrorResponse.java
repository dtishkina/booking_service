package com.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Стандартный формат ответа об ошибке")
public record ErrorResponse(
        @Schema(description = "Момент времени возникновения ошибки",
                example = "2025-12-04T10:15:30Z")
        OffsetDateTime timestamp,

        @Schema(description = "HTTP статус", example = "404")
        int status,

        @Schema(description = "Краткое текстовое описание статуса", example = "Not Found")
        String error,

        @Schema(description = "Сообщение об ошибке", example = "Resource not found: id=1")
        String message,

        @Schema(description = "Путь запроса", example = "/api/resources/1")
        String path
) {}
