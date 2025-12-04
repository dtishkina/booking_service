package com.example.booking.controller;

import com.example.booking.dto.ErrorResponse;
import com.example.booking.dto.ResourceCreateRequest;
import com.example.booking.dto.ResourceDto;
import com.example.booking.dto.ResourceUpdateRequest;
import com.example.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@Tag(name = "Resources", description = "Управление ресурсами для бронирования")
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    @Operation(
            summary = "Получить список ресурсов",
            description = "Возвращает список ресурсов. По умолчанию только активные ресурсы."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список ресурсов успешно получен"
    )
    public List<ResourceDto> getAllResources(
            @Parameter(
                    description = "Если true, возвращаются только активные ресурсы",
                    example = "true"
            )
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly
    ) {
        if (activeOnly) {
            return resourceService.getAllActive();
        }
        return resourceService.getAll();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Получить ресурс по ID",
            description = "Возвращает информацию о ресурсе по его идентификатору."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ресурс найден"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ресурс не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResourceDto getResource(
            @Parameter(description = "Идентификатор ресурса", example = "1")
            @PathVariable Long id
    ) {
        return resourceService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Создать ресурс",
            description = "Создаёт новый ресурс для бронирования (переговорку, рабочее место и т.п.)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ресурс успешно создан"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResourceDto createResource (@RequestBody @Valid ResourceCreateRequest request) {
        return resourceService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Обновить ресурс",
            description = "Полностью обновляет данные ресурса по его идентификатору."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ресурс успешно обновлён"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные запроса",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ресурс не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResourceDto updateResource(
            @Parameter(description = "Идентификатор ресурса", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid ResourceUpdateRequest request) {
        return resourceService.update(id, request);
    }

    @PatchMapping("/{id}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Деактивировать ресурс",
            description = "Помечает ресурс как неактивный. Не удаляет его из системы."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ресурс успешно деактивирован"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Ресурс не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public void deactivateResource(
            @Parameter(description = "Идентификатор ресурса", example = "1")
            @PathVariable Long id
    ) {
        resourceService.deactivate(id);
    }
}
