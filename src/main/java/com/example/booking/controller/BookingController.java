package com.example.booking.controller;

import com.example.booking.dto.BookingCreateRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.ErrorResponse;
import com.example.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Bookings", description = "Управление бронированиями ресурсов")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/api/users/{userId}/bookings")
    @Operation(
            summary = "Получить бронирования пользователя",
            description = "Возвращает список всех бронирований, созданных указанным пользователем."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список бронирований пользователя"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public List<BookingResponse> getBookingsForUser(
            @Parameter(description = "Идентификатор пользователя", example = "1")
            @PathVariable Long userId
    ) {
        return bookingService.getBookingsForUser(userId);
    }

    @PostMapping("/api/users/{userId}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Создать бронирование",
            description = "Создаёт новое бронирование ресурса от имени пользователя."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Бронирование успешно создано"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса или неверный интервал времени",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь или ресурс не найдены",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Конфликт бронирования (ресурс уже занят в этот период)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public BookingResponse createBooking(
            @Parameter(description = "Идентификатор пользователя", example = "1")
            @PathVariable Long userId,
            @RequestBody @Valid BookingCreateRequest request
    ) {
        return bookingService.createBooking(userId, request);
    }

    @DeleteMapping("/api/users/{userId}/bookings/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Отменить бронирование",
            description = "Отменяет бронирование. Сейчас отмена разрешена только владельцу брони."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Бронирование успешно отменено"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь или бронирование не найдены",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Пользователь не является владельцем бронирования",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public void cancelBooking(
            @Parameter(description = "Идентификатор пользователя", example = "1")
            @PathVariable Long userId,
            @Parameter(description = "Идентификатор бронирования", example = "10")
            @PathVariable Long bookingId
    ) {
        bookingService.cancelBooking(userId, bookingId);
    }

    @GetMapping("/api/resources/{resourceId}/bookings")
    @Operation(
            summary = "Получить активные бронирования ресурса",
            description = "Возвращает список активных бронирований для указанного ресурса."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список бронирований ресурса"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ресурс не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public List<BookingResponse> getBookingsForResource(
            @Parameter(description = "Идентификатор ресурса", example = "1")
            @PathVariable Long resourceId
    ) {
        return bookingService.getBookingsForResource(resourceId);
    }
}
