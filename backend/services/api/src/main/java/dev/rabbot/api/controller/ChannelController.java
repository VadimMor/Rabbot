package dev.rabbot.api.controller;

import dev.rabbot.api.dto.request.ChannelSettingsRequest;
import dev.rabbot.api.dto.response.ChannelResponse;
import dev.rabbot.api.services.ChannelService;
import dev.rabbot.api.utils.CurrentTenant;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Пути двух видов:
//   /brands/{brandId}/channels — каналы внутри бренда (вложенный ресурс);
//   /channels/{id}             — конкретный канал (id уникален сам по себе).
@RestController
@RequestMapping("/api/v1")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping("/brands/{brandId}/channels")
    public List<ChannelResponse> listByBrand(@CurrentTenant UUID tenantId, @PathVariable UUID brandId) {
        return channelService.listByBrand(tenantId, brandId);
    }

    @GetMapping("/channels/{id}")
    public ChannelResponse get(@CurrentTenant UUID tenantId, @PathVariable UUID id) {
        return channelService.get(tenantId, id);
    }

    // PATCH, а не PUT: меняем часть полей.
    @PatchMapping("/channels/{id}")
    public ChannelResponse updateSettings(@CurrentTenant UUID tenantId,
                                          @PathVariable UUID id,
                                          @Valid @RequestBody ChannelSettingsRequest request) {
        return channelService.updateSettings(tenantId, id, request);
    }

    // Действие над ресурсом — отдельный POST-путь с глаголом.
    @PostMapping("/channels/{id}/disconnect")
    public ChannelResponse disconnect(@CurrentTenant UUID tenantId, @PathVariable UUID id) {
        return channelService.disconnect(tenantId, id);
    }
}