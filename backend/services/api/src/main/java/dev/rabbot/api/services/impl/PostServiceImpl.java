package dev.rabbot.api.services.impl;

import dev.rabbot.api.dto.request.PostCreateRequest;
import dev.rabbot.api.dto.request.PostUpdateRequest;
import dev.rabbot.api.dto.request.VariantRequest;
import dev.rabbot.api.dto.request.VariantTextRequest;
import dev.rabbot.api.dto.response.PageResponse;
import dev.rabbot.api.dto.response.PostResponse;
import dev.rabbot.api.dto.response.PostSummaryResponse;
import dev.rabbot.api.model.Brand;
import dev.rabbot.api.model.Post;
import dev.rabbot.api.model.PostVariant;
import dev.rabbot.api.enums.PostStatus;
import dev.rabbot.api.enums.SocialPlatform;
import dev.rabbot.api.repository.BrandRepository;
import dev.rabbot.api.repository.PostRepository;
import dev.rabbot.api.services.PostService;
import dev.rabbot.api.utils.ConflictException;
import dev.rabbot.api.utils.NotFoundException;
import dev.rabbot.api.utils.PostMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final BrandRepository brandRepository;
    private final PostMapper postMapper;
    private final EntityManager entityManager;
    private final PostLoader postLoader;

    public PostServiceImpl(PostRepository postRepository,
                           BrandRepository brandRepository,
                           PostMapper postMapper,
                           EntityManager entityManager,
                           PostLoader postLoader) {
        this.postRepository = postRepository;
        this.brandRepository = brandRepository;
        this.postMapper = postMapper;
        this.entityManager = entityManager;
        this.postLoader = postLoader;
    }

    @Override
    public PageResponse<PostSummaryResponse> listByBrand(UUID tenantId, UUID brandId,
                                                         PostStatus status, int page, int size) {
        if (!brandRepository.existsByIdAndTenantId(brandId, tenantId)) {
            throw new NotFoundException("Бренд " + brandId + " не найден");
        }

        // Сортировка совпадает с индексом post_brand_status_idx (brand_id, status, updated_at DESC).
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<Post> posts = (status == null)
                ? postRepository.findAllByTenantIdAndBrand_Id(tenantId, brandId, pageable)
                : postRepository.findAllByTenantIdAndBrand_IdAndStatus(tenantId, brandId, status, pageable);

        return PageResponse.of(posts.map(postMapper::toSummary));
    }

    @Override
    public PostResponse get(UUID tenantId, UUID id) {
        Post post = postRepository.findWithVariantsByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Пост " + id + " не найден"));
        return postMapper.toResponse(post);
    }

    @Override
    @Transactional
    public PostResponse create(UUID tenantId, UUID brandId, PostCreateRequest request) {
        // Бренд нужен целиком: из него берём tenantId для поста.
        Brand brand = brandRepository.findByIdAndTenantId(brandId, tenantId)
                .orElseThrow(() -> new NotFoundException("Бренд " + brandId + " не найден"));

        Post post = new Post(brand, request.kind(), request.title().strip(), request.trendId());

        if (request.variants() != null) {
            for (VariantRequest v : request.variants()) {
                post.addVariant(v.platform(), v.body(), v.hashtags());
            }
        }

        // Один save: варианты сохранятся каскадом (cascade = ALL).
        // flush — чтобы БД вернула created_at, char_count и т. п. до ответа.
        Post saved = postRepository.saveAndFlush(post);
        return postMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PostResponse update(UUID tenantId, UUID id, PostUpdateRequest request) {
        Post post = postLoader.loadChecked(tenantId, id, request.version());

        // Клиент видел старую версию → кто-то уже изменил пост → 409.
        // Это ловит конфликт "открыл давно, сохранил сейчас".
        // Одновременную гонку двух запросов поймает сам @Version при UPDATE.
        if (post.getVersion() != request.version()) {
            throw new ConflictException("Пост уже изменён (версия " + post.getVersion()
                    + "). Обновите страницу и повторите.");
        }

        post.rename(request.title().strip());
        return postMapper.toResponse(postRepository.saveAndFlush(post));
    }

        @Override
    @Transactional
    public PostResponse addVariant(UUID tenantId, UUID postId, int version, VariantRequest request) {
        Post post = loadForChange(tenantId, postId, version);
        post.addVariant(request.platform(), request.body(), request.hashtags());
        return postLoader.saveAndReload(post);
    }

    @Override
    @Transactional
    public PostResponse editVariant(UUID tenantId, UUID postId, UUID variantId, int version,
                                    VariantTextRequest request) {
        Post post = loadForChange(tenantId, postId, version);
        PostVariant variant = post.findVariant(variantId);
        post.editVariant(variant, request.body(), request.hashtags());
        return postLoader.saveAndReload(post);
    }

    @Override
    @Transactional
    public PostResponse selectVariant(UUID tenantId, UUID postId, UUID variantId, int version) {
        Post post = loadForChange(tenantId, postId, version);
        PostVariant variant = post.findVariant(variantId);

        if (!variant.isSelected()) {
            // Шаг 1: снять старый выбор и сразу записать в БД.
            post.unselectPlatform(variant.getPlatform());
            entityManager.flush();
            // Шаг 2: выбрать новый. Теперь уникальный индекс не сработает.
            post.selectVariant(variant);
        }
        return postLoader.saveAndReload(post);
    }

    @Override
    @Transactional
    public PostResponse deleteVariant(UUID tenantId, UUID postId, UUID variantId, int version) {
        Post post = loadForChange(tenantId, postId, version);
        PostVariant variant = post.findVariant(variantId);
        SocialPlatform platform = variant.getPlatform();
        boolean wasSelected = variant.isSelected();

        post.removeVariant(variant);
        // DELETE сейчас — до того, как выберем замену.
        entityManager.flush();

        // Удалили выбранный — выбираем оставшийся вариант с наименьшим номером,
        // чтобы у площадки не пропал текст для публикации.
        if (wasSelected) {
            post.firstVariantOf(platform).ifPresent(post::selectVariant);
        }
        return postLoader.saveAndReload(post);
    }

    // ---------- помощники ----------
    private Post loadForChange(UUID tenantId, UUID postId, int version) {
        Post post = postLoader.loadChecked(tenantId, postId, version);
        post.requireEditable();
        entityManager.lock(post, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        return post;
    }
}