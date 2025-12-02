package com.example.booking.service;

import com.example.booking.dto.ResourceCreateRequest;
import com.example.booking.dto.ResourceDto;
import com.example.booking.dto.ResourceUpdateRequest;
import com.example.booking.entity.Resource;
import com.example.booking.exception.NotFoundException;
import com.example.booking.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ResourceService {
    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Transactional(readOnly = true)
    public List<ResourceDto> getAllActive() {
        return resourceRepository.findByActiveTrue()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceDto> getAll() {
        return resourceRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResourceDto getById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found: id = " + id));
        return toDto(resource);
    }

    public ResourceDto create(ResourceCreateRequest request){
        Resource resource = new Resource();
        resource.setName(request.name());
        resource.setType(request.type());
        resource.setCapacity(request.capacity());
        resource.setLocation(request.location());
        resource.setActive(true);

        Resource saved = resourceRepository.save(resource);
        return toDto(saved);
    }

    public ResourceDto update(Long id, ResourceUpdateRequest request){
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found: id = " + id));
        resource.setName(request.name());
        resource.setType(request.type());
        resource.setCapacity(request.capacity());
        resource.setLocation(request.location());
        resource.setActive(request.active());

        Resource saved = resourceRepository.save(resource);
        return toDto(saved);
    }

    public void deactivate(Long id){
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found: id = " + id));
        resource.setActive(false);
        resourceRepository.save(resource);
    }

    private ResourceDto toDto(Resource resource) {
        return new ResourceDto(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getCapacity(),
                resource.getLocation(),
                resource.isActive()
        );
    }
}
