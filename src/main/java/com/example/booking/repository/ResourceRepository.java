package com.example.booking.repository;

import com.example.booking.entity.Resource;
import com.example.booking.entity.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByActiveTrue();
    List<Resource> findByTypeAndActiveTrue(ResourceType type);
}