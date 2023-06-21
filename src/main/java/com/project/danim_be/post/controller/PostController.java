package com.project.danim_be.post.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.danim_be.common.Anotation.LogExecutionTime;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.post.dto.RequestDto.ImageRequestDto;
import com.project.danim_be.post.dto.RequestDto.PostRequestDto;
import com.project.danim_be.post.service.PostService;
import com.project.danim_be.post.service.SearchService;
import com.project.danim_be.security.auth.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "PostController", description = "게시글 API")
@RestController
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;
	private final SearchService searchService;

	@Operation(summary = "게시글 작성 API", description = "게시글 작성")
	@PostMapping(value = "api/post",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@LogExecutionTime
	public ResponseEntity<Message> createPost(@AuthenticationPrincipal final UserDetailsImpl userDetails,@Valid @ModelAttribute final PostRequestDto requestDto){
			return	postService.createPost(userDetails.getMember(),requestDto);

	}
	@Operation(summary = "이미지 업로드 API", description = "이미지 업로드")
	@PostMapping(value = "api/post/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Message> imageUpload(@ModelAttribute ImageRequestDto requestDto){
		return	postService.imageUpload(requestDto);

	}
	@Operation(summary = "게시글 조회 API", description = "게시글 조회")
	@GetMapping("api/post/{postId}")
	@LogExecutionTime
	public ResponseEntity<Message> readPost(@PathVariable("postId") Long id) throws JsonProcessingException {
		return searchService.readPost(id);
	}

	@Operation(summary = "게시글 수정 API", description = "게시글 수정")
	@PutMapping(value = "api/post/{postId}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@LogExecutionTime
	public ResponseEntity<Message> updatePost(@PathVariable("postId") Long id, @AuthenticationPrincipal final UserDetailsImpl userDetails, @ModelAttribute final PostRequestDto requestDto){
		System.out.println(userDetails.getMember());
		return postService.updatePost(id, userDetails.getMember(), requestDto);
	}
	@Operation(summary = "게시글 삭제 API", description = "게시글 삭제")
	@DeleteMapping("api/post/{postId}")
	@LogExecutionTime
	public ResponseEntity<Message> deletePost(@PathVariable("postId") Long id, @AuthenticationPrincipal final UserDetailsImpl userDetails){

		return postService.deletePost(id, userDetails.getMember());
	}

}
