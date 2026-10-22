package dev.rabbot.api.services.impl;

import dev.rabbot.api.dto.request.BrandRequest;
import dev.rabbot.api.dto.response.BrandResponse;
import dev.rabbot.api.dto.response.PageResponse;
import dev.rabbot.api.model.Brand;
import dev.rabbot.api.repository.BrandRepository;
import dev.rabbot.api.services.BrandService;
import dev.rabbot.api.utils.BrandMapper;
import dev.rabbot.api.utils.ConflictException;
import dev.rabbot.api.utils.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// @Service — Spring создаёт один экземпляр (бин) и внедряет его в контроллер.
// По умолчанию все методы в транзакции только для чтения: так дешевле для БД.
@Service
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    // Внедрение через конструктор: поля final, зависимости видны явно.
    public BrandServiceImpl(BrandRepository brandRepository, BrandMapper brandMapper) {
        this.brandRepository = brandRepository;
        this.brandMapper = brandMapper;
    }

    @Override
    public PageResponse<BrandResponse> list(UUID tenantId, int page, int size) {
        // Новые бренды первыми.
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // map() превращает каждую сущность в DTO, не теряя данные о страницах.
        var result = brandRepository.findAllByTenantId(tenantId, pageable)
                .map(brandMapper::toResponse);
        return PageResponse.of(result);
    }

    @Override
    public BrandResponse get(UUID tenantId, UUID id) {
        return brandMapper.toResponse(findOrThrow(tenantId, id));
    }

    @Override
    @Transactional // запись: обычная транзакция (не readOnly)
    public BrandResponse create(UUID tenantId, BrandRequest request) {
        String name = request.name().strip();

        // Понятная ошибка 409 вместо падения на UNIQUE (tenant_id, name) в БД.
        if (brandRepository.existsByTenantIdAndName(tenantId, name)) {
            throw new ConflictException("Бренд с именем '" + name + "' уже существует");
        }

        Brand brand = brandMapper.toNewEntity(tenantId, request);
        // saveAndFlush сразу выполняет INSERT, чтобы Hibernate перечитал
        // created_at/updated_at до того, как мы соберём ответ.
        Brand saved = brandRepository.saveAndFlush(brand);
        return brandMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BrandResponse update(UUID tenantId, UUID id, BrandRequest request) {
        Brand brand = findOrThrow(tenantId, id);
        String name = request.name().strip();

        if (brandRepository.existsByTenantIdAndNameAndIdNot(tenantId, name, id)) {
            throw new ConflictException("Бренд с именем '" + name + "' уже существует");
        }

        // Меняем поля загруженной сущности. Hibernate сам заметит изменения
        // (dirty checking) и выполнит UPDATE.
        brandMapper.apply(brand, request);
        // flush — чтобы UPDATE ушёл сейчас и updated_at из триггера попал в ответ.
        Brand saved = brandRepository.saveAndFlush(brand);
        return brandMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID id) {
        // Сначала проверяем, что бренд есть и принадлежит организации,
        // иначе вернём 404.
        Brand brand = findOrThrow(tenantId, id);
        brandRepository.delete(brand);
    }

    // Общий метод: найти бренд организации или бросить 404.
    private Brand findOrThrow(UUID tenantId, UUID id) {
        return brandRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Бренд " + id + " не найден"));
    }
}