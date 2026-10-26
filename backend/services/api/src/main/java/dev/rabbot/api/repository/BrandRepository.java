package dev.rabbot.api.repository;

import dev.rabbot.api.model.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Spring Data сам пишет реализацию: SQL строится по имени метода.
// JpaRepository уже даёт save, delete, findById и т. д.
public interface BrandRepository extends JpaRepository<Brand, UUID> {

    // SELECT ... WHERE tenant_id = ? ORDER BY ... LIMIT/OFFSET
    Page<Brand> findAllByTenantId(UUID tenantId, Pageable pageable);

    // Ищем бренд только внутри своей организации, иначе чужой бренд
    // можно было бы получить, зная его id.
    Optional<Brand> findByIdAndTenantId(UUID id, UUID tenantId);

    // Есть ли уже бренд с таким именем в организации (для создания).
    boolean existsByTenantIdAndName(UUID tenantId, String name);

    // То же, но не считая сам изменяемый бренд (для обновления).
    boolean existsByTenantIdAndNameAndIdNot(UUID tenantId, String name, UUID id);

    // Принадлежит ли бренд организации (без загрузки самого бренда).
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
}