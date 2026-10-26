package dev.rabbot.api.services;

import dev.rabbot.api.dto.request.ChannelSettingsRequest;
import dev.rabbot.api.dto.response.ChannelResponse;

import java.util.List;
import java.util.UUID;

public interface ChannelService {

    List<ChannelResponse> listByBrand(UUID tenantId, UUID brandId);

    ChannelResponse get(UUID tenantId, UUID id);

    ChannelResponse updateSettings(UUID tenantId, UUID id, ChannelSettingsRequest request);

    ChannelResponse disconnect(UUID tenantId, UUID id);
}