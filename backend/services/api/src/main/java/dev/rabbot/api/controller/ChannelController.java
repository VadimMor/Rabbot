package dev.rabbot.api.controller;

import dev.rabbot.api.dto.request.ChannelSettingsRequest;
import dev.rabbot.api.dto.response.ChannelResponse;
import dev.rabbot.api.services.ChannelService;
import dev.rabbot.api.utils.CurrentTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Каналы", description = "Подключённые аккаунты соцсетей. Создаются через OAuth (позже)")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping("/brands/{brandId}/channels")
    @Operation(summary = "Получить список каналов бренда", description = "Возвращает список каналов, подключённых к конкретному бренду")
    public List<ChannelResponse> listByBrand(@CurrentTenant UUID tenantId, @PathVariable UUID brandId) {
        return channelService.listByBrand(tenantId, brandId);
    }

    @GetMapping("/channels/{id}")
    @Operation(summary = "Получить канал по ID", description = "Возвращает канал по его уникальному идентификатору")
    public ChannelResponse get(@CurrentTenant UUID tenantId, @PathVariable UUID id) {
        return channelService.get(tenantId, id);
    }

    // PATCH, а не PUT: меняем часть полей.
    @PatchMapping("/channels/{id}")
    @Operation(summary = "Обновить настройки канала", description = "Обновляет настройки конкретного канала по его уникальному идентификатору")
    public ChannelResponse updateSettings(@CurrentTenant UUID tenantId,
                                          @PathVariable UUID id,
                                          @Valid @RequestBody ChannelSettingsRequest request) {
        return channelService.updateSettings(tenantId, id, request);
    }

    // Действие над ресурсом — отдельный POST-путь с глаголом.
    @PostMapping("/channels/{id}/disconnect")
    @Operation(summary = "Отключить канал", description = "Отключает конкретный канал по его уникальному идентификатору")
    public ChannelResponse disconnect(@CurrentTenant UUID tenantId, @PathVariable UUID id) {
        return channelService.disconnect(tenantId, id);
    }
}