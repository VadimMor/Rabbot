package dev.rabbot.api.services.impl;

import dev.rabbot.api.dto.response.PageResponse;
import dev.rabbot.api.dto.response.PostResponse;
import dev.rabbot.api.dto.response.PostSummaryResponse;
import dev.rabbot.api.model.Post;
import dev.rabbot.api.enums.PostStatus;
import dev.rabbot.api.repository.BrandRepository;
import dev.rabbot.api.repository.PostRepository;
import dev.rabbot.api.services.PostService;
import dev.rabbot.api.utils.NotFoundException;
import dev.rabbot.api.utils.PostMapper;
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

    public PostServiceImpl(PostRepository postRepository,
                           BrandRepository brandRepository,
                           PostMapper postMapper) {
        this.postRepository = postRepository;
        this.brandRepository = brandRepository;
        this.postMapper = postMapper;
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
}