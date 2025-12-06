package com.example.booking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

@Schema(description = "Запрос на создание бронирования ресурса")
public record BookingCreateRequest(
        @NotNull
        @Schema(description = "Идентификатор ресурса для бронирования", example = "1")
        Long resourceId,

        @NotNull
        @Schema(
                description = """
                        Время начала бронирования в формате ISO-8601 с указанием часового пояса.
                        Не может быть в прошлом. Минимальная длительность бронирования и максимальное
                        количество дней вперёд задаются в конфигурации приложения.
                        Пример: 2025-12-05T13:00:00+03:00
                        """,
                example = "2025-12-05T13:00:00+03:00"
        )
        OffsetDateTime startTime,

        @NotNull
        @Schema(
                description = "Время окончания бронирования в формате ISO-8601 с указанием часового пояса",
                example = "2025-12-05T15:00:00+03:00"
        )
        OffsetDateTime endTime
) {}