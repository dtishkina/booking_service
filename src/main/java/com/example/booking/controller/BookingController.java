package com.example.booking.controller;

import com.example.booking.dto.BookingCreateRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/api/users/{userId}/bookings")
    public List<BookingResponse> getBookingsForUser(@PathVariable Long userId) {
        return bookingService.getBookingsForUser(userId);
    }

    @PostMapping("/api/users/{userId}/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@PathVariable Long userId,
                                         @RequestBody @Valid BookingCreateRequest request) {
        return bookingService.createBooking(userId, request);
    }

    @DeleteMapping("/api/users/{userId}/bookings/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelBooking(@PathVariable Long userId,
                              @PathVariable Long bookingId) {
        bookingService.cancelBooking(userId, bookingId);
    }

    @GetMapping("/api/resources/{resourceId}/bookings")
    public List<BookingResponse> getBookingsForResource(@PathVariable Long resourceId) {
        return bookingService.getBookingsForResource(resourceId);
    }
}
