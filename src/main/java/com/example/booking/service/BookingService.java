package com.example.booking.service;

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

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          ResourceRepository resourceRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
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

        OffsetDateTime start = request.startTime();
        OffsetDateTime end = request.endTime();

        if(start == null || end == null || !start.isBefore(end)) {
            throw new BookingConflictException("Invalid time range: start must be before end");
        }

        var overlapping = bookingRepository
                .findByResourceAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                        resource,
                        BookingStatus.ACTIVE,
                        end,
                        start
                );

        if (!overlapping.isEmpty()) {
            throw new BookingConflictException("Resource is already booked in this time range");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setResource(resource);
        booking.setStartTime(start);
        booking.setEndTime(end);
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
}
