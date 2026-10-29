package dev.rabbot.api.controller;

import dev.rabbot.api.dto.request.BrandRequest;
import dev.rabbot.api.dto.response.BrandResponse;
import dev.rabbot.api.dto.response.PageResponse;
import dev.rabbot.api.services.BrandService;
import dev.rabbot.api.utils.CurrentTenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

// REST-контроллер: принимает HTTP, отдаёт JSON. Логики здесь нет,
// только приём параметров и вызов сервиса.
@RestController
@RequestMapping("/api/v1/brands")
@Tag(name = "Бренды", description = "Профиль бренда: аудитория, тон, запрещённые слова, правила стиля")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    // GET /api/v1/brands?page=0&size=20
    // @Min/@Max на параметрах Spring проверяет сам (встроенная валидация методов).
    @GetMapping
    @Operation(summary = "Получить список брендов", description = "Возвращает список брендов с пагинацией")
    public PageResponse<BrandResponse> list(
            @CurrentTenant UUID tenantId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return brandService.list(tenantId, page, size);
    }

    // GET /api/v1/brands/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Получить бренд по ID", description = "Возвращает бренд по его уникальному идентификатору")
    public BrandResponse get(@CurrentTenant UUID tenantId, @PathVariable UUID id) {
        return brandService.get(tenantId, id);
    }

    // POST /api/v1/brands → 201 Created + заголовок Location с адресом нового бренда.
    @PostMapping
    @Operation(summary = "Создать бренд", description = "Создаёт новый бренд")
    public ResponseEntity<BrandResponse> create(
            @CurrentTenant UUID tenantId,
            @Valid @RequestBody BrandRequest request,
            UriComponentsBuilder uriBuilder) {
        BrandResponse created = brandService.create(tenantId, request);
        var location = uriBuilder.path("/api/v1/brands/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // PUT /api/v1/brands/{id}: полная замена полей бренда.
    @PutMapping("/{id}")
    @Operation (summary = "Обновить бренд", description = "Обновляет существующий бренд по его уникальному идентификатору")
    public BrandResponse update(
            @CurrentTenant UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody BrandRequest request) {
        return brandService.update(tenantId, id, request);
    }

    // DELETE /api/v1/brands/{id} → 204 No Content.
    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить бренд", description = "Удаляет бренд по его уникальному идентификатору")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentTenant UUID tenantId, @PathVariable UUID id) {
        brandService.delete(tenantId, id);
    }
}