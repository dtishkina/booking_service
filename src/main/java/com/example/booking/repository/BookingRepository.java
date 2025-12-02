package com.example.booking.repository;

import com.example.booking.entity.Booking;
import com.example.booking.entity.BookingStatus;
import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUser(User user);

    List<Booking> findByResourceAndStatus(Resource resource, BookingStatus status);

    List<Booking> findByResourceAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Resource resource,
            BookingStatus status,
            OffsetDateTime end,
            OffsetDateTime start
    );
}