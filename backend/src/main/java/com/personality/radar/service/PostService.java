package com.personality.radar.service;

import com.personality.radar.common.BusinessException;
import com.personality.radar.domain.*;
import com.personality.radar.dto.ApiDtos;
import com.personality.radar.repository.*;
import java.util.*;
import java.util.Collections;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepository posts;
    private final PostInteractionRepository interactions;
    private final PostCommentRepository comments;
    private final TestResultRepository results;
    private final MockAiService mockAi;
    private final SensitiveWordFilter wordFilter;

    public PostService(PostRepository posts, PostInteractionRepository interactions,
                       PostCommentRepository comments, TestResultRepository results,
                       MockAiService mockAi, SensitiveWordFilter wordFilter) {
        this.posts = posts;
        this.interactions = interactions;
        this.comments = comments;
        this.results = results;
        this.mockAi = mockAi;
        this.wordFilter = wordFilter;
    }

    @Transactional
    public ApiDtos.PostResponse create(UserAccount user, ApiDtos.CreatePostRequest request) {
        if (results.findFirstByUserAndTypeOrderByCreatedAtDesc(user, TestType.PERSONALITY).isEmpty()) {
            throw new BusinessException(403, "请先完成性格测试再发布动态");
        }
        Post post = new Post();
        post.setAuthor(user);
        post.setContent(request.content());
        post.setDomainTag(DomainTag.valueOf(request.domainTag().toUpperCase()));
        post.setStyleTags(toJsonArray(request.styleTags()));
        post.setAiVector(mockAi.generateVector(request.content()));
        post.setAiReviewStatus(Post.ReviewStatus.APPROVED);
        post.setActivePost(true);
        posts.save(post);
        return toResponse(post, user);
    }

    @Transactional(readOnly = true)
    public ApiDtos.PostListResponse list(UserAccount user, String sort, String domain, int page) {
        var pageable = PageRequest.of(page, 20);
        List<Post> postList = domain != null && !domain.isBlank()
                ? posts.findByActiveTrueAndActivePostTrueAndDomainTagOrderByCreatedAtDesc(
                        DomainTag.valueOf(domain.toUpperCase()), pageable)
                : posts.findByActiveTrueAndActivePostTrueOrderByCreatedAtDesc(pageable);

        Map<String, Integer> userScores = null;
        if ("recommend".equals(sort)) {
            userScores = results.findFirstByUserAndTypeOrderByCreatedAtDesc(user, TestType.PERSONALITY)
                    .map(r -> new HashMap<>(r.getScores())).orElse(null);
        }

        List<ApiDtos.PostResponse> items = new ArrayList<>();
        for (Post post : postList) {
            int compatibility = userScores != null
                    ? (int) Math.round(MatchEngine.compatibilityScore(userScores, post.getAiVector()))
                    : 0;
            items.add(toResponse(post, user, compatibility, userScores != null));
        }

        if ("recommend".equals(sort) && userScores != null) {
            // Sort by compatibility desc, tiebreak by interaction count desc
            items.sort((a, b) -> {
                int cmp = Integer.compare(b.compatibility(), a.compatibility());
                if (cmp != 0) return cmp;
                // Hotness tiebreaker: likes + comments + favorites from the post entity
                Post pa = posts.findById(a.id()).orElse(null);
                Post pb = posts.findById(b.id()).orElse(null);
                int hotA = pa != null ? pa.getLikeCount() + pa.getCommentCount() + pa.getFavoriteCount() : 0;
                int hotB = pb != null ? pb.getLikeCount() + pb.getCommentCount() + pb.getFavoriteCount() : 0;
                return Integer.compare(hotB, hotA);
            });
            // Information cocoon breaking: mix 1-2 low-compat posts from positions 11-30
            if (items.size() > 10) {
                List<ApiDtos.PostResponse> top10 = new ArrayList<>(items.subList(0, Math.min(10, items.size())));
                List<ApiDtos.PostResponse> tail = new ArrayList<>(items.subList(Math.min(10, items.size()), items.size()));
                if (tail.size() >= 2) {
                    Collections.shuffle(tail);
                    top10.add(tail.get(0));
                    if (tail.size() >= 2) top10.add(tail.get(1));
                } else if (tail.size() == 1) {
                    top10.add(tail.get(0));
                }
                items = top10;
            }
        }
        return new ApiDtos.PostListResponse(items, items.size());
    }

    @Transactional
    public ApiDtos.PostResponse getById(Long id, UserAccount viewer) {
        Post post = posts.findById(id).orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        post.setViewCount(post.getViewCount() + 1);
        posts.save(post);
        Map<String, Integer> userScores = results
                .findFirstByUserAndTypeOrderByCreatedAtDesc(viewer, TestType.PERSONALITY)
                .map(r -> new HashMap<>(r.getScores())).orElse(null);
        int compatibility = userScores != null
                ? (int) Math.round(MatchEngine.compatibilityScore(userScores, post.getAiVector())) : 0;
        return toResponse(post, viewer, compatibility, userScores != null);
    }

    @Transactional
    public ApiDtos.PostResponse update(UserAccount user, Long id, ApiDtos.CreatePostRequest request) {
        Post post = posts.findById(id).orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new BusinessException(403, "只能编辑自己的帖子");
        }
        post.setContent(request.content());
        post.setDomainTag(DomainTag.valueOf(request.domainTag().toUpperCase()));
        post.setStyleTags(toJsonArray(request.styleTags()));
        post.setAiVector(mockAi.generateVector(request.content()));
        post.setUpdatedAt(java.time.Instant.now());
        posts.save(post);
        return toResponse(post, user);
    }

    @Transactional
    public void delete(UserAccount user, Long id) {
        Post post = posts.findById(id).orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        boolean isAuthor = post.getAuthor().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;
        if (!isAuthor && !isAdmin) {
            throw new BusinessException(403, "只能删除自己的帖子");
        }
        post.setActive(false);
        posts.save(post);
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.MyCommentResponse> myComments(UserAccount user, int page) {
        var pageable = PageRequest.of(page, 50);
        return comments.findByUserAndActiveTrueOrderByCreatedAtDesc(user, pageable).stream()
                .map(c -> {
                    String postContent = c.getPost().getContent();
                    String postTitle = postContent.length() > 40 ? postContent.substring(0, 40) + "..." : postContent;
                    return new ApiDtos.MyCommentResponse(
                            c.getId(), c.getContent(),
                            c.getPost().getId(), postTitle,
                            c.getCreatedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ApiDtos.PostListResponse myPosts(UserAccount user, int page) {
        var pageable = PageRequest.of(page, 20);
        List<Post> postList = posts.findByAuthorOrderByCreatedAtDesc(user, pageable);
        List<ApiDtos.PostResponse> items = new ArrayList<>();
        for (Post post : postList) {
            items.add(toResponse(post, user, 0, false));
        }
        return new ApiDtos.PostListResponse(items, items.size());
    }

    @Transactional
    public void like(UserAccount user, Long postId) {
        Post post = posts.findById(postId).orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        if (interactions.findByUserAndPostAndType(user, post, PostInteraction.InteractionType.LIKE).isPresent()) {
            return;
        }
        PostInteraction interaction = new PostInteraction();
        interaction.setUser(user); interaction.setPost(post);
        interaction.setType(PostInteraction.InteractionType.LIKE);
        interactions.save(interaction);
        post.setLikeCount(post.getLikeCount() + 1);
        recalculateVector(post);
        posts.save(post);
    }

    @Transactional
    public void unlike(UserAccount user, Long postId) {
        Post post = posts.findById(postId).orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        var existing = interactions.findByUserAndPostAndType(user, post, PostInteraction.InteractionType.LIKE);
        if (existing.isEmpty()) return;
        interactions.delete(existing.get());
        post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        recalculateVector(post);
        posts.save(post);
    }

    private void recalculateVector(Post post) {
        // Post vector = average of all liking users' personality vectors
        List<PostInteraction> likes = interactions.findByPostAndType(post, PostInteraction.InteractionType.LIKE);
        if (likes.isEmpty()) {
            // Fall back to AI initial vector — already stored, no change needed
            return;
        }
        Map<String, double[]> sums = new HashMap<>();
        int[] count = {0};
        for (PostInteraction like : likes) {
            var scores = results.findFirstByUserAndTypeOrderByCreatedAtDesc(like.getUser(), TestType.PERSONALITY);
            if (scores.isEmpty()) continue;
            count[0]++;
            scores.get().getScores().forEach((dim, val) ->
                    sums.computeIfAbsent(dim, k -> new double[1])[0] += val);
        }
        if (count[0] == 0) return;
        Map<String, Integer> avgVector = new HashMap<>();
        int totalCount = count[0];
        sums.forEach((dim, arr) -> avgVector.put(dim, (int) Math.round(arr[0] / totalCount)));
        post.setAiVector(avgVector);
    }

    @Transactional
    public void favorite(UserAccount user, Long postId) {
        Post post = posts.findById(postId).orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        if (interactions.findByUserAndPostAndType(user, post, PostInteraction.InteractionType.FAVORITE).isPresent()) {
            return;
        }
        PostInteraction interaction = new PostInteraction();
        interaction.setUser(user);
        interaction.setPost(post);
        interaction.setType(PostInteraction.InteractionType.FAVORITE);
        interactions.save(interaction);
        post.setFavoriteCount(post.getFavoriteCount() + 1);
        posts.save(post);
    }

    @Transactional
    public void unfavorite(UserAccount user, Long postId) {
        Post post = posts.findById(postId).orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        var existing = interactions.findByUserAndPostAndType(user, post, PostInteraction.InteractionType.FAVORITE);
        if (existing.isEmpty()) return;
        interactions.delete(existing.get());
        post.setFavoriteCount(Math.max(0, post.getFavoriteCount() - 1));
        posts.save(post);
    }

    @Transactional(readOnly = true)
    public ApiDtos.PostListResponse favorites(UserAccount user, int page) {
        var pageable = PageRequest.of(page, 20);
        List<PostInteraction> favs = interactions.findByUserAndTypeOrderByCreatedAtDesc(
                user, PostInteraction.InteractionType.FAVORITE, pageable);
        List<ApiDtos.PostResponse> items = favs.stream()
                .map(pi -> toResponse(pi.getPost(), user, 0, false))
                .toList();
        return new ApiDtos.PostListResponse(items, items.size());
    }

    @Transactional
    public void batchUnfavorite(UserAccount user, List<Long> postIds) {
        List<Post> postList = posts.findAllById(postIds);
        interactions.deleteByUserAndPostInAndType(user, postList, PostInteraction.InteractionType.FAVORITE);
    }

    @Transactional
    public ApiDtos.CommentResponse createComment(UserAccount user, Long postId, String content) {
        if (wordFilter.containsSensitiveWord(content)) {
            throw new BusinessException(400, "评论含不当内容，请修改");
        }
        Post post = posts.findById(postId).orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);
        comments.save(comment);
        post.setCommentCount(post.getCommentCount() + 1);
        posts.save(post);
        return new ApiDtos.CommentResponse(comment.getId(), comment.getContent(),
                DtoMapper.user(user), comment.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.CommentResponse> listComments(Long postId, int page) {
        Post post = posts.findById(postId).orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        var pageable = PageRequest.of(page, 10);
        return comments.findByPostAndActiveTrueOrderByCreatedAtAsc(post, pageable).stream()
                .map(c -> new ApiDtos.CommentResponse(c.getId(), c.getContent(),
                        DtoMapper.user(c.getUser()), c.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void updateComment(UserAccount user, Long commentId, String content) {
        PostComment comment = comments.findById(commentId)
                .orElseThrow(() -> new BusinessException(404, "评论不存在"));
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new BusinessException(403, "只能修改自己的评论");
        }
        comment.setContent(content);
        comments.save(comment);
    }

    @Transactional
    public void deleteComment(UserAccount user, Long commentId) {
        PostComment comment = comments.findById(commentId)
                .orElseThrow(() -> new BusinessException(404, "评论不存在"));
        boolean isAuthor = comment.getUser().getId().equals(user.getId());
        boolean isPostOwner = comment.getPost().getAuthor().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;
        if (!isAuthor && !isPostOwner && !isAdmin) {
            throw new BusinessException(403, "无权删除此评论");
        }
        comment.setActive(false);
        comments.save(comment);
    }

    private String toJsonArray(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "[]";
        return "[" + tags.stream().map(t -> "\"" + t + "\"").reduce((a, b) -> a + "," + b).orElse("") + "]";
    }

    private List<String> fromJsonArray(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return List.of();
        return Arrays.stream(json.replace("[", "").replace("]", "").replace("\"", "").split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private ApiDtos.PostResponse toResponse(Post post, UserAccount viewer) {
        return toResponse(post, viewer, 0, false);
    }

    private ApiDtos.PostResponse toResponse(Post post, UserAccount viewer, int compatibility, boolean showCompatibility) {
        List<String> tags = fromJsonArray(post.getStyleTags());
        return new ApiDtos.PostResponse(
                post.getId(), DtoMapper.user(post.getAuthor()), post.getContent(),
                post.getImages(), post.getDomainTag().name(), tags,
                new HashMap<>(post.getAiVector()), post.getAiReviewStatus().name(),
                post.getLikeCount(), post.getFavoriteCount(), post.getCommentCount(),
                post.getViewCount(), showCompatibility ? compatibility : 0,
                showCompatibility, post.getCreatedAt(), post.getUpdatedAt());
    }
}
