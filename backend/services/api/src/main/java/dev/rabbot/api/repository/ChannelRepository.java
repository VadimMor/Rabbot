package dev.rabbot.api.repository;

import dev.rabbot.api.model.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    // Brand_Id — путь по связи: channel.brand.id (то есть колонка brand_id).
    // Каналов у бренда единицы, поэтому без пагинации.
    List<Channel> findAllByTenantIdAndBrand_IdOrderByConnectedAtAsc(UUID tenantId, UUID brandId);

    Optional<Channel> findByIdAndTenantId(UUID id, UUID tenantId);
}