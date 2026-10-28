package dev.rabbot.api.repository;

import dev.rabbot.api.model.PostRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PostRevisionRepository extends JpaRepository<PostRevision, Long> {

    // Top50 - LIMIT 50. Сортируем по id, а не по created_at:
    // id из IDENTITY строго растёт, а у двух ревизий из одной транзакции
    // created_at одинаковый (now() — время начала транзакции).
    List<PostRevision> findTop50ByVariant_IdOrderByIdDesc(UUID variantId);
}