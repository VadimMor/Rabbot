package dev.rabbot.api.services.impl;

import dev.rabbot.api.dto.request.ChannelSettingsRequest;
import dev.rabbot.api.dto.response.ChannelResponse;
import dev.rabbot.api.model.Channel;
import dev.rabbot.api.repository.BrandRepository;
import dev.rabbot.api.repository.ChannelRepository;
import dev.rabbot.api.services.ChannelService;
import dev.rabbot.api.utils.ChannelMapper;
import dev.rabbot.api.utils.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;
    private final BrandRepository brandRepository;
    private final ChannelMapper channelMapper;

    public ChannelServiceImpl(ChannelRepository channelRepository,
                              BrandRepository brandRepository,
                              ChannelMapper channelMapper) {
        this.channelRepository = channelRepository;
        this.brandRepository = brandRepository;
        this.channelMapper = channelMapper;
    }

    @Override
    public List<ChannelResponse> listByBrand(UUID tenantId, UUID brandId) {
        // Чужой или несуществующий бренд → 404, а не пустой список:
        // так клиент отличит "каналов нет" от "бренда нет".
        if (!brandRepository.existsByIdAndTenantId(brandId, tenantId)) {
            throw new NotFoundException("Бренд " + brandId + " не найден");
        }
        return channelRepository.findAllByTenantIdAndBrand_IdOrderByConnectedAtAsc(tenantId, brandId)
                .stream()
                .map(channelMapper::toResponse)
                .toList();
    }

    @Override
    public ChannelResponse get(UUID tenantId, UUID id) {
        return channelMapper.toResponse(findOrThrow(tenantId, id));
    }

    @Override
    @Transactional
    public ChannelResponse updateSettings(UUID tenantId, UUID id, ChannelSettingsRequest request) {
        Channel channel = findOrThrow(tenantId, id);

        // PATCH: меняем только переданные поля.
        if (request.autoPublish() != null) {
            channel.setAutoPublish(request.autoPublish());
        }
        if (request.dailyPostLimit() != null) {
            channel.setDailyPostLimit(request.dailyPostLimit());
        }
        if (request.timezone() != null) {
            channel.setTimezone(validZone(request.timezone()));
        }

        return channelMapper.toResponse(channelRepository.saveAndFlush(channel));
    }

    @Override
    @Transactional
    public ChannelResponse disconnect(UUID tenantId, UUID id) {
        Channel channel = findOrThrow(tenantId, id);
        // Логика смены статуса живёт в самой сущности, а не в сервисе.
        channel.disconnect();
        return channelMapper.toResponse(channelRepository.saveAndFlush(channel));
    }

    private Channel findOrThrow(UUID tenantId, UUID id) {
        return channelRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Канал " + id + " не найден"));
    }

    // Проверяем, что такой часовой пояс существует в Java (а значит, и в PostgreSQL).
    private static String validZone(String zone) {
        String value = zone.strip();
        if (!ZoneId.getAvailableZoneIds().contains(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Неизвестный часовой пояс: " + value);
        }
        return value;
    }
}