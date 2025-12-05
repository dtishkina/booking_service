package com.example.booking.controller;

import com.example.booking.dto.BookingCreateRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.dto.ErrorResponse;
import com.example.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin/Manager Bookings", description = "Админские и менеджерские операции с бронированиями")
@SecurityRequirement(name = "bearerAuth")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(
            summary = "Получить все бронирования",
            description = "Возвращает список всех бронирований в системе. Доступно только менеджеру или администратору."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список всех бронирований"),
            @ApiResponse(
                    responseCode = "403",
                    description = "Нет прав доступа",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public List<BookingResponse> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/users/{userId}/bookings")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(
            summary = "Получить бронирования пользователя",
            description = "Возвращает список бронирований указанного пользователя. Доступно менеджеру или администратору."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список бронирований пользователя"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Нет прав доступа",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public List<BookingResponse> getUserBookings(
            @Parameter(description = "Идентификатор пользователя", example = "5")
            @PathVariable Long userId
    ) {
        return bookingService.getBookingsForUser(userId);
    }

    @PostMapping("/users/{userId}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(
            summary = "Создать бронирование от имени пользователя",
            description = "Создаёт бронирование для указанного пользователя. Доступно менеджеру или администратору."
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
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Нет прав доступа",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public BookingResponse createBookingForUser(
            @Parameter(description = "Идентификатор пользователя, для которого создаётся бронирование", example = "5")
            @PathVariable Long userId,
            @RequestBody @Valid BookingCreateRequest request
    ) {
        return bookingService.createBookingForUser(userId, request);
    }

    @DeleteMapping("/bookings/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    @Operation(
            summary = "Отменить бронирование (админ/менеджер)",
            description = "Отменяет любое бронирование независимо от владельца. Доступно менеджеру или администратору."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Бронирование успешно отменено"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Бронирование не найдено",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Нет прав доступа",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public void cancelBookingAsAdmin(
            @Parameter(description = "Идентификатор бронирования", example = "10")
            @PathVariable Long bookingId
    ) {
        bookingService.cancelBookingAsAdmin(bookingId);
    }
}
