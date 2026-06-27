package com.personality.radar.controller;

import com.personality.radar.common.ApiResponse;
import com.personality.radar.dto.ApiDtos;
import com.personality.radar.service.CurrentUserService;
import com.personality.radar.service.PostService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final CurrentUserService currentUser;
    private final PostService postService;

    public PostController(CurrentUserService currentUser, PostService postService) {
        this.currentUser = currentUser;
        this.postService = postService;
    }

    @PostMapping
    public ApiResponse<ApiDtos.PostResponse> create(@Valid @RequestBody ApiDtos.CreatePostRequest request) {
        return ApiResponse.ok(postService.create(currentUser.requireUser(), request));
    }

    @GetMapping
    public ApiResponse<ApiDtos.PostListResponse> list(
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String domain,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(postService.list(currentUser.requireUser(), sort, domain, page));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApiDtos.PostResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(postService.getById(id, currentUser.requireUser()));
    }

    @PutMapping("/{id}")
    public ApiResponse<ApiDtos.PostResponse> update(@PathVariable Long id, @Valid @RequestBody ApiDtos.CreatePostRequest request) {
        return ApiResponse.ok(postService.update(currentUser.requireUser(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        postService.delete(currentUser.requireUser(), id);
        return ApiResponse.ok();
    }

    @GetMapping("/mine")
    public ApiResponse<ApiDtos.PostListResponse> mine(@RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(postService.myPosts(currentUser.requireUser(), page));
    }

    @GetMapping("/comments/mine")
    public ApiResponse<List<ApiDtos.MyCommentResponse>> myComments(@RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(postService.myComments(currentUser.requireUser(), page));
    }

    @PutMapping("/comments/{commentId}")
    public ApiResponse<Void> updateComment(@PathVariable Long commentId, @RequestBody ApiDtos.UpdateCommentRequest request) {
        postService.updateComment(currentUser.requireUser(), commentId, request.content());
        return ApiResponse.ok();
    }

    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteOwnComment(@PathVariable Long commentId) {
        postService.deleteComment(currentUser.requireUser(), commentId);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/like")
    public ApiResponse<Void> like(@PathVariable Long id) {
        postService.like(currentUser.requireUser(), id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}/like")
    public ApiResponse<Void> unlike(@PathVariable Long id) {
        postService.unlike(currentUser.requireUser(), id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/favorite")
    public ApiResponse<Void> favorite(@PathVariable Long id) {
        postService.favorite(currentUser.requireUser(), id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}/favorite")
    public ApiResponse<Void> unfavorite(@PathVariable Long id) {
        postService.unfavorite(currentUser.requireUser(), id);
        return ApiResponse.ok();
    }

    @GetMapping("/favorites")
    public ApiResponse<ApiDtos.PostListResponse> favorites(@RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(postService.favorites(currentUser.requireUser(), page));
    }

    @DeleteMapping("/favorites/batch")
    public ApiResponse<Void> batchUnfavorite(@RequestBody List<Long> ids) {
        postService.batchUnfavorite(currentUser.requireUser(), ids);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<ApiDtos.CommentResponse> createComment(@PathVariable Long id, @Valid @RequestBody ApiDtos.CreateCommentRequest request) {
        return ApiResponse.ok(postService.createComment(currentUser.requireUser(), id, request.content()));
    }

    @GetMapping("/{id}/comments")
    public ApiResponse<List<ApiDtos.CommentResponse>> listComments(@PathVariable Long id, @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(postService.listComments(id, page));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(@PathVariable Long id, @PathVariable Long commentId) {
        postService.deleteComment(currentUser.requireUser(), commentId);
        return ApiResponse.ok();
    }
}
