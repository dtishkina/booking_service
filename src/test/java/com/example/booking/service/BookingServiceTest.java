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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResourceRepository resourceRepository;

    private BookingProperties bookingProperties;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingProperties = new BookingProperties(
                30L,
                8L,
                30L
        );

        bookingService = new BookingService(
                bookingRepository,
                userRepository,
                resourceRepository,
                bookingProperties
        );
    }


    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setEmail("user" + id + "@example.com");
        user.setPasswordHash("hash");
        user.setEnabled(true);
        return user;
    }

    private Resource createResource(Long id, boolean active) {
        Resource resource = new Resource();
        resource.setId(id);
        resource.setName("Resource " + id);
        resource.setActive(active);
        return resource;
    }

    private Booking createBooking(Long id, User user, Resource resource,
                                  OffsetDateTime start, OffsetDateTime end, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUser(user);
        booking.setResource(resource);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setStatus(status);
        return booking;
    }

    @Test
    void getBookingsForUser_shouldReturnBookingsOfUser() {
        Long userId = 1L;
        User user = createUser(userId);
        Resource resource = createResource(10L, true);

        OffsetDateTime start = OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MINUTES);
        OffsetDateTime end = start.plusHours(2);

        Booking booking = createBooking(100L, user, resource, start, end, BookingStatus.ACTIVE);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookingRepository.findByUser(user)).willReturn(List.of(booking));

        List<BookingResponse> result = bookingService.getBookingsForUser(userId);

        assertThat(result).hasSize(1);
        BookingResponse dto = result.get(0);
        assertThat(dto.id()).isEqualTo(100L);
        assertThat(dto.userId()).isEqualTo(userId);
        assertThat(dto.resourceId()).isEqualTo(resource.getId());
        assertThat(dto.resourceName()).isEqualTo(resource.getName());
        assertThat(dto.status()).isEqualTo(BookingStatus.ACTIVE);
    }

    @Test
    void getBookingsForUser_userNotFound_shouldThrowNotFound() {
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> bookingService.getBookingsForUser(userId));
    }

    @Test
    void getBookingsForResource_shouldReturnActiveBookings() {
        Long resourceId = 10L;
        Resource resource = createResource(resourceId, true);
        User user = createUser(1L);

        OffsetDateTime start = OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MINUTES);
        OffsetDateTime end = start.plusHours(2);

        Booking booking = createBooking(100L, user, resource, start, end, BookingStatus.ACTIVE);

        given(resourceRepository.findById(resourceId)).willReturn(Optional.of(resource));
        given(bookingRepository.findByResourceAndStatus(resource, BookingStatus.ACTIVE))
                .willReturn(List.of(booking));

        List<BookingResponse> result = bookingService.getBookingsForResource(resourceId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(BookingStatus.ACTIVE);
    }

    @Test
    void getBookingsForResource_resourceNotFound_shouldThrowNotFound() {
        Long resourceId = 10L;
        given(resourceRepository.findById(resourceId)).willReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> bookingService.getBookingsForResource(resourceId));
    }

    @Test
    void getAllBookings_shouldReturnAllBookings() {
        User user = createUser(1L);
        Resource resource = createResource(10L, true);

        OffsetDateTime start = OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MINUTES);
        OffsetDateTime end = start.plusHours(1);

        Booking booking1 = createBooking(1L, user, resource, start, end, BookingStatus.ACTIVE);
        Booking booking2 = createBooking(2L, user, resource, start.plusDays(1), end.plusDays(1), BookingStatus.CANCELLED);

        given(bookingRepository.findAll()).willReturn(List.of(booking1, booking2));

        List<BookingResponse> result = bookingService.getAllBookings();

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(BookingResponse::id)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void createBooking_shouldCreateBooking_whenNoConflicts() {
        Long userId = 1L;
        Long resourceId = 10L;

        User user = createUser(userId);
        Resource resource = createResource(resourceId, true);

        OffsetDateTime start = OffsetDateTime.now()
                .plusDays(1)
                .withSecond(30)
                .withNano(123);
        OffsetDateTime end = start.plusHours(2).plusSeconds(10);

        BookingCreateRequest request = new BookingCreateRequest(
                resourceId,
                start,
                end
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(resourceRepository.findById(resourceId)).willReturn(Optional.of(resource));

        given(bookingRepository.findByResourceAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(resource),
                eq(BookingStatus.ACTIVE),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).willReturn(List.of());

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        given(bookingRepository.save(any(Booking.class))).willAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(100L);
            return b;
        });

        BookingResponse response = bookingService.createBooking(userId, request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.resourceId()).isEqualTo(resourceId);
        assertThat(response.status()).isEqualTo(BookingStatus.ACTIVE);

        verify(bookingRepository).save(bookingCaptor.capture());
        Booking saved = bookingCaptor.getValue();

        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getResource()).isEqualTo(resource);
        assertThat(saved.getStartTime().getSecond()).isZero();
        assertThat(saved.getStartTime().getNano()).isZero();
        assertThat(saved.getEndTime().getSecond()).isZero();
        assertThat(saved.getEndTime().getNano()).isZero();
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.ACTIVE);
    }

    @Test
    void createBooking_shouldThrow_whenResourceInactive() {
        Long userId = 1L;
        Long resourceId = 10L;

        User user = createUser(userId);
        Resource resource = createResource(resourceId, false); // не активен

        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        OffsetDateTime end = start.plusHours(1);

        BookingCreateRequest request = new BookingCreateRequest(
                resourceId,
                start,
                end
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(resourceRepository.findById(resourceId)).willReturn(Optional.of(resource));

        assertThrows(BookingConflictException.class,
                () -> bookingService.createBooking(userId, request));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_shouldThrow_whenOverlappingExists() {
        Long userId = 1L;
        Long resourceId = 10L;

        User user = createUser(userId);
        Resource resource = createResource(resourceId, true);

        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        OffsetDateTime end = start.plusHours(1);

        BookingCreateRequest request = new BookingCreateRequest(
                resourceId,
                start,
                end
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(resourceRepository.findById(resourceId)).willReturn(Optional.of(resource));

        // уже есть пересекающееся бронирование
        given(bookingRepository.findByResourceAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(resource),
                eq(BookingStatus.ACTIVE),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).willReturn(List.of(new Booking()));

        assertThrows(BookingConflictException.class,
                () -> bookingService.createBooking(userId, request));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_shouldCancel_whenUserIsOwnerAndActive() {
        Long userId = 1L;
        Long bookingId = 100L;

        User user = createUser(userId);
        Resource resource = createResource(10L, true);

        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        OffsetDateTime end = start.plusHours(1);

        Booking booking = createBooking(bookingId, user, resource, start, end, BookingStatus.ACTIVE);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookingRepository.findById(bookingId)).willReturn(Optional.of(booking));

        bookingService.cancelBooking(userId, bookingId);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBooking_shouldThrow_whenUserIsNotOwner() {
        Long userId = 1L;
        Long bookingId = 100L;

        User owner = createUser(2L);
        User otherUser = createUser(userId);
        Resource resource = createResource(10L, true);

        Booking booking = createBooking(
                bookingId,
                owner,
                resource,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(1),
                BookingStatus.ACTIVE
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(otherUser));
        given(bookingRepository.findById(bookingId)).willReturn(Optional.of(booking));

        assertThrows(BookingConflictException.class,
                () -> bookingService.cancelBooking(userId, bookingId));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_shouldReturnWithoutSaving_whenAlreadyCancelled() {
        Long userId = 1L;
        Long bookingId = 100L;

        User user = createUser(userId);
        Resource resource = createResource(10L, true);

        Booking booking = createBooking(
                bookingId,
                user,
                resource,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(1),
                BookingStatus.CANCELLED
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(bookingRepository.findById(bookingId)).willReturn(Optional.of(booking));

        bookingService.cancelBooking(userId, bookingId);

        // статус уже CANCELLED, save не должен вызываться
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBookingAsAdmin_shouldCancelWithoutUserCheck() {
        Long bookingId = 100L;
        User user = createUser(1L);
        Resource resource = createResource(10L, true);

        Booking booking = createBooking(
                bookingId,
                user,
                resource,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(1),
                BookingStatus.ACTIVE
        );

        given(bookingRepository.findById(bookingId)).willReturn(Optional.of(booking));

        bookingService.cancelBookingAsAdmin(bookingId);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBookingAsAdmin_shouldReturnWithoutSaving_whenAlreadyCancelled() {
        Long bookingId = 100L;
        User user = createUser(1L);
        Resource resource = createResource(10L, true);

        Booking booking = createBooking(
                bookingId,
                user,
                resource,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(1),
                BookingStatus.CANCELLED
        );

        given(bookingRepository.findById(bookingId)).willReturn(Optional.of(booking));

        bookingService.cancelBookingAsAdmin(bookingId);

        verify(bookingRepository, never()).save(any());
    }
}
