package dev.rabbot.api.utils;

import dev.rabbot.api.dto.response.ChannelResponse;
import dev.rabbot.api.model.Channel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChannelMapper {

    public ChannelResponse toResponse(Channel channel) {
        return new ChannelResponse(
                channel.getId(),
                // У LAZY-связи getId() не вызывает запрос в БД.
                channel.getBrand().getId(),
                channel.getPlatform(),
                channel.getExternalAccountId(),
                channel.getHandle(),
                channel.getDisplayName(),
                channel.getStatus(),
                channel.isAutoPublish(),
                channel.getDailyPostLimit(),
                channel.getTimezone(),
                channel.getTokenExpiresAt(),
                List.copyOf(channel.getScopes()),
                channel.getAudienceSize(),
                channel.getConnectedAt(),
                channel.getLastError(),
                channel.getCreatedAt(),
                channel.getUpdatedAt()
        );
    }
}