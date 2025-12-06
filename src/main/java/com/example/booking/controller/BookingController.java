package com.example.booking.controller;

import com.example.booking.dto.BookingCreateRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.ErrorResponse;
import com.example.booking.security.CustomUserDetails;
import com.example.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Bookings", description = "Управление бронированиями ресурсов")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/api/bookings/my")
    @PreAuthorize("hasAnyRole('USER','MANAGER','ADMIN')")
    @Operation(
            summary = "Получить мои бронирования",
            description = "Возвращает список всех бронирований, созданных текущим аутентифицированным пользователем."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список бронирований пользователя"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Пользователь не аутентифицирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public List<BookingResponse> getBookingsForUser(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return bookingService.getBookingsForUser(currentUser.getId());
    }

    @PostMapping("/api/bookings")
    @PreAuthorize("hasAnyRole('USER','MANAGER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Создать бронирование",
            description = """
                Создаёт новое бронирование ресурса от имени пользователя.

                Ограничения:
                - нельзя бронировать в прошлом;
                - минимальная длительность бронирования (в минутах) задаётся параметром
                  booking.min-duration-minutes;
                - максимальная длительность бронирования (в часах) задаётся параметром
                  booking.max-duration-hours;
                - нельзя бронировать дальше, чем на N дней вперёд, где N задаётся параметром
                  booking.max-booking-days-ahead;
                - если время начала и окончания перепутаны местами, они автоматически меняются местами;
                - секунды и наносекунды обнуляются (используются только минуты).
                """
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
                    description = "Ресурс не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Конфликт бронирования (ресурс уже занят в этот период)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public BookingResponse createBooking(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Valid BookingCreateRequest request
    ) {
        return bookingService.createBooking(currentUser.getId(), request);
    }

    @DeleteMapping("/api/bookings/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('USER','MANAGER','ADMIN')")
    @Operation(
            summary = "Отменить бронирование",
            description = "Отменяет бронирование, если текущий пользователь является его владельцем."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Бронирование успешно отменено"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Бронирование не найдено",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Пользователь не является владельцем бронирования",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public void cancelBooking(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "Идентификатор бронирования", example = "10")
            @PathVariable Long bookingId
    ) {
        bookingService.cancelBooking(currentUser.getId(), bookingId);
    }

    @GetMapping("/api/resources/{resourceId}/bookings")
    @PreAuthorize("hasAnyRole('USER','MANAGER','ADMIN')")
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
