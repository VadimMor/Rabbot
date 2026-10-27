package dev.rabbot.api.services.impl;

import dev.rabbot.api.dto.response.PostResponse;
import dev.rabbot.api.model.Post;
import dev.rabbot.api.repository.PostRepository;
import dev.rabbot.api.utils.ConflictException;
import dev.rabbot.api.utils.NotFoundException;
import dev.rabbot.api.utils.PostMapper;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Общие операции загрузки и сохранения поста для PostServiceImpl
// и PostWorkflowServiceImpl. Вызывается внутри их транзакций.
@Component
class PostLoader {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final EntityManager entityManager;

    PostLoader(PostRepository postRepository, PostMapper postMapper, EntityManager entityManager) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
        this.entityManager = entityManager;
    }

    Post findWithVariantsOrThrow(UUID tenantId, UUID id) {
        return postRepository.findWithVariantsByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Пост " + id + " не найден"));
    }

    // Загрузить пост и проверить, что клиент видел актуальную версию.
    Post loadChecked(UUID tenantId, UUID id, int version) {
        Post post = findWithVariantsOrThrow(tenantId, id);
        if (post.getVersion() != version) {
            throw new ConflictException("Пост уже изменён (версия " + post.getVersion()
                    + "). Обновите страницу и повторите.");
        }
        return post;
    }

    // Записать всё в БД и прочитать пост заново без блокировок.
    PostResponse saveAndReload(Post post) {
        UUID tenantId = post.getTenantId();
        UUID id = post.getId();
        entityManager.flush();
        entityManager.clear();
        return postMapper.toResponse(findWithVariantsOrThrow(tenantId, id));
    }
}