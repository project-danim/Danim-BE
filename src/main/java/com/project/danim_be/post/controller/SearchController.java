package com.project.danim_be.post.controller;

import com.project.danim_be.common.Anotation.LogExecutionTime;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.common.util.StatusEnum;
import com.project.danim_be.post.dto.RequestDto.SearchRequestDto;
import com.project.danim_be.post.dto.ResponseDto.CardPostResponseDto;
import com.project.danim_be.post.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "SearchController", description = "검색 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/posts")
public class SearchController {

    private final SearchService searchService;

    @Operation(summary = "전체 게시글 조회 API", description = "전체 페이지 조회")
    @GetMapping("/")
    @LogExecutionTime
    public ResponseEntity<Message> allPosts(Pageable pageable) {

        return searchService.allPosts(pageable);

    }

    @Operation(summary = "게시글 상세 검색 API", description = "게시글 상세 검색")
    @PostMapping("/search")
    @LogExecutionTime
    public ResponseEntity<Message> searchPosts(@RequestBody SearchRequestDto searchRequestDto, Pageable pageable){

        List<CardPostResponseDto> searchResult = searchService.searchPost(searchRequestDto, pageable);
        return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "검색 결과", searchResult));

    }
}
