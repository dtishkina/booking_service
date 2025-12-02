package com.example.booking.controller;

import com.example.booking.dto.ResourceCreateRequest;
import com.example.booking.dto.ResourceDto;
import com.example.booking.dto.ResourceUpdateRequest;
import com.example.booking.service.ResourceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public List<ResourceDto> getAllResources(@RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly) {
        if (activeOnly) {
            return resourceService.getAllActive();
        }
        return resourceService.getAll();
    }

    @GetMapping("/{id}")
    public ResourceDto getResource(@PathVariable Long id) {
        return resourceService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceDto createResource (@RequestBody @Valid ResourceCreateRequest request) {
        return resourceService.create(request);
    }

    @PutMapping("/{id}")
    public ResourceDto updateResource(@PathVariable Long id,
                                      @RequestBody @Valid ResourceUpdateRequest request) {
        return resourceService.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateResource(@PathVariable Long id) {
        resourceService.deactivate(id);
    }
}
