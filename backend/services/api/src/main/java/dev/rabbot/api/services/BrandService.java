package dev.rabbot.api.services;

import dev.rabbot.api.dto.request.BrandRequest;
import dev.rabbot.api.dto.response.BrandResponse;
import dev.rabbot.api.dto.response.PageResponse;

import java.util.UUID;

// Контракт бизнес-логики. Контроллер знает только интерфейс,
// поэтому реализацию легко подменить, например моком в тестах.
public interface BrandService {

    PageResponse<BrandResponse> list(UUID tenantId, int page, int size);

    BrandResponse get(UUID tenantId, UUID id);

    BrandResponse create(UUID tenantId, BrandRequest request);

    BrandResponse update(UUID tenantId, UUID id, BrandRequest request);

    void delete(UUID tenantId, UUID id);
}