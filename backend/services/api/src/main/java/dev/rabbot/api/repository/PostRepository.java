package dev.rabbot.api.repository;

import dev.rabbot.api.model.Post;
import dev.rabbot.api.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    // Все посты бренда.
    Page<Post> findAllByTenantIdAndBrand_Id(UUID tenantId, UUID brandId, Pageable pageable);

    // Посты бренда с фильтром по статусу.
    Page<Post> findAllByTenantIdAndBrand_IdAndStatus(UUID tenantId, UUID brandId,
                                                     PostStatus status, Pageable pageable);

    // @EntityGraph: загрузить пост и его варианты ОДНИМ запросом (LEFT JOIN),
    // а не "пост, потом отдельный SELECT вариантов".
    // "WithVariants" в имени Spring Data игнорирует — это просто подсказка читателю.
    @EntityGraph(attributePaths = "variants")
    Optional<Post> findWithVariantsByIdAndTenantId(UUID id, UUID tenantId);
}