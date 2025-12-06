package com.example.booking.service;

import com.example.booking.config.BookingProperties;
import com.example.booking.dto.BookingCreateRequest;
import com.example.booking.dto.BookingResponse;
import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.exception.BookingConflictException;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.BookingRepository;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class BookingService {
    private record BookingInterval(
            OffsetDateTime start,
            OffsetDateTime end
    ) {}

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final BookingProperties bookingProperties;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          ResourceRepository resourceRepository, BookingProperties bookingProperties) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.bookingProperties = bookingProperties;
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForUser (Long userId) {
        User user = getUserOrThrow(userId);
        return bookingRepository.findByUser(user)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForResource (Long resourceId) {
        Resource resource = getResourceOrThrow(resourceId);
        return bookingRepository.findByResourceAndStatus(resource, BookingStatus.ACTIVE)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public BookingResponse createBooking(Long userId, BookingCreateRequest request) {
        User user = getUserOrThrow(userId);
        Resource resource = getResourceOrThrow(request.resourceId());

        if(!resource.isActive()) {
            throw new BookingConflictException("Resource is not active: id = " + resource.getId());
        }

        BookingInterval interval = normalizeAndValidateInterval(
                request.startTime(),
                request.endTime()
        );

        var overlapping = bookingRepository
                .findByResourceAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                        resource,
                        BookingStatus.ACTIVE,
                        interval.end(),
                        interval.start()
                );

        if (!overlapping.isEmpty()) {
            throw new BookingConflictException("Resource is already booked in this time range");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setResource(resource);
        booking.setStartTime(interval.start());
        booking.setEndTime(interval.end());
        booking.setStatus(BookingStatus.ACTIVE);

        Booking saved = bookingRepository.save(booking);
        return toDto(saved);
    }

    public BookingResponse createBookingForUser(Long userId, BookingCreateRequest request) {
        return createBooking(userId, request);
    }

    public void cancelBooking(Long userId, Long bookingId) {
        User user = getUserOrThrow(userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: id=" + bookingId));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new BookingConflictException("User is not owner of this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    public void cancelBookingAsAdmin(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: id = " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: id=" + userId));
    }

    private Resource getResourceOrThrow(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found: id=" + resourceId));
    }

    private BookingResponse toDto(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getUser().getId(),
                booking.getResource().getId(),
                booking.getResource().getName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus()
        );
    }

    private BookingInterval normalizeAndValidateInterval(OffsetDateTime start,
                                                         OffsetDateTime end) {
        if (start == null || end == null) {
            throw new BookingConflictException("Start and end time must be provided");
        }

        OffsetDateTime normalizedStart = start.withSecond(0).withNano(0);
        OffsetDateTime normalizedEnd = end.withSecond(0).withNano(0);

        if (normalizedEnd.isBefore(normalizedStart)) {
            OffsetDateTime tmp = normalizedStart;
            normalizedStart = normalizedEnd;
            normalizedEnd = tmp;
        }

        if (!normalizedStart.isBefore(normalizedEnd)) {
            throw new BookingConflictException("Start time must be before end time");
        }

        OffsetDateTime now = OffsetDateTime.now().withSecond(0).withNano(0);

        if (normalizedStart.isBefore(now)) {
            throw new BookingConflictException("Cannot create booking in the past");
        }

        long maxDaysAhead = bookingProperties.maxBookingDaysAhead();
        if (normalizedStart.isAfter(now.plusDays(maxDaysAhead))) {
            throw new BookingConflictException(
                    "Cannot create booking more than " + maxDaysAhead + " days ahead"
            );
        }

        long minutes = Duration.between(normalizedStart, normalizedEnd).toMinutes();

        long minMinutes = bookingProperties.minDurationMinutes();
        if (minutes < minMinutes) {
            throw new BookingConflictException(
                    "Booking duration must be at least " + minMinutes + " minutes"
            );
        }

        long maxMinutes = bookingProperties.maxDurationHours() * 60;
        if (minutes > maxMinutes) {
            throw new BookingConflictException(
                    "Booking duration must not exceed " + bookingProperties.maxDurationHours() + " hours"
            );
        }

        return new BookingInterval(normalizedStart, normalizedEnd);
    }


}
