package com.project.danim_be.post.service;

import com.project.danim_be.chat.entity.MemberChatRoom;
import com.project.danim_be.chat.repository.MemberChatRoomRepository;
import com.project.danim_be.common.CacheService;
import com.project.danim_be.common.exception.CustomException;
import com.project.danim_be.common.exception.ErrorCode;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.common.util.StatusEnum;
import com.project.danim_be.post.dto.RequestDto.SearchRequestDto;
import com.project.danim_be.post.dto.ResponseDto.CardPostResponseDto;
import com.project.danim_be.post.dto.ResponseDto.PostResponseDto;
import com.project.danim_be.post.entity.Post;
import com.project.danim_be.post.entity.QImage;
import com.project.danim_be.post.entity.QPost;
import com.project.danim_be.post.repository.PostRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final JPAQueryFactory queryFactory;
    private final PostRepository postRepository;
    private final MemberChatRoomRepository memberChatRoomRepository;
    @Autowired
    private CacheService cacheService;

    //전체 조회
    @Transactional(readOnly = true)
    public ResponseEntity<Message> allPosts(Pageable pageable){
        QPost qPost = QPost.post;
        QImage qImage = QImage.image;
//        NumberExpression<Integer> condition = new CaseBuilder().when(qPost.groupSize.eq(qPost.numberOfParticipants))
//                .then(1)
//                .otherwise(0);

        List<CardPostResponseDto> cardPostResponseDtoList = queryFactory
                .select(Projections.constructor(CardPostResponseDto.class,
                        qPost.id,
                        qPost.postTitle,
                        qPost.recruitmentEndDate,
                        qPost.member.nickname,
                        qPost.numberOfParticipants,
                        qPost.groupSize,
                        qPost.location,
                        qPost.keyword,
                        qPost.ageRange,
                        JPAExpressions.select(qImage.imageUrl.min().coalesce("https://danimdata.s3.ap-northeast-2.amazonaws.com/Frame+2448+(2).png"))
                                .from(qImage)
                                .where(qImage.post.id.eq(qPost.id))
                                .orderBy(qImage.id.asc()),
                        qPost.gender,
                        qPost.isRecruitmentEnd,
                        qPost.member.imageUrl))
                .from(qPost)
                .where(qPost.isDeleted.eq(false))
                .orderBy(qPost.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "전체 데이터 조회 성공", cardPostResponseDtoList));

//        List<CardPostResponseDto> cardPostResponseDtoList = new ArrayList<>();
//
//        for (Post post : postList) {
//            cardPostResponseDtoList.add(new CardPostResponseDto(post));
//        }
//
//        return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "전체 데이터 조회성공", cardPostResponseDtoList));
    }

    // 상세 검색
    @Transactional(readOnly = true)
    public List<CardPostResponseDto> searchPost(SearchRequestDto searchRequestDto, Pageable pageable) {

        // QueryDSL을 활용하여 동적 쿼리 작성
        BooleanBuilder predicate = new BooleanBuilder();
        QPost qPost = QPost.post;
        QImage qImage = QImage.image;
        // 지역에 관한 필터
        if (searchRequestDto.getLocation() != null) {
            predicate.and(qPost.location.eq(searchRequestDto.getLocation()));
        }
        // 제목+내용의 검색필터
        if (searchRequestDto.getSearchKeyword() != null) {
            BooleanBuilder searchKeywordPredicate = new BooleanBuilder();
            searchKeywordPredicate.or(qPost.postTitle.containsIgnoreCase(searchRequestDto.getSearchKeyword()));
            searchKeywordPredicate.or(qPost.content.containsIgnoreCase(searchRequestDto.getSearchKeyword()));
            predicate.and(searchKeywordPredicate);
        }
        // 모집인원의 검색필터
        if (searchRequestDto.getGroupSize() != null){
            predicate.and(qPost.groupSize.eq(searchRequestDto.getGroupSize()));
        }
        // 성별에 대한 검색 필터
        if (searchRequestDto.getGender() != null){
            String[] genderList = searchRequestDto.getGender().split(",");
            BooleanBuilder genderPredicate = new BooleanBuilder();
            for (String gender : genderList) {
                genderPredicate.or(qPost.gender.containsIgnoreCase(gender));
            }
            predicate.and(genderPredicate);
        }
        // 나이대에 대한 검색필터
        if (searchRequestDto.getAgeRange() != null) {
            String[] ageRangeList = searchRequestDto.getAgeRange().split(",");
            BooleanBuilder ageRangePredicate = new BooleanBuilder();
            for (String ageRange : ageRangeList) {
                ageRangePredicate.or(qPost.ageRange.containsIgnoreCase(ageRange));
            }
            predicate.and(ageRangePredicate);
        }
        //키워드에 대한 검색필터
        if (searchRequestDto.getKeyword() != null) {
            String[] keywordList = searchRequestDto.getKeyword().split(",");
            BooleanBuilder keywordPredicate = new BooleanBuilder();
            for (String keyword : keywordList) {
                keywordPredicate.or(qPost.keyword.containsIgnoreCase(keyword));
            }
            predicate.and(keywordPredicate);
        }

        // 모집 마감글에 대한 필터
        if (!searchRequestDto.getExceptCompletedPost()){
            predicate.and(qPost.numberOfParticipants.ne(qPost.groupSize));
            predicate.and(qPost.isRecruitmentEnd.eq(false));
        }

        predicate.and(qPost.isDeleted.eq(false));

        // 동적 쿼리 실행

        return queryFactory
                .select(Projections.constructor(CardPostResponseDto.class,
                        qPost.id,
                        qPost.postTitle,
                        qPost.recruitmentEndDate,
                        qPost.member.nickname,
                        qPost.numberOfParticipants,
                        qPost.groupSize,
                        qPost.location,
                        qPost.keyword,
                        qPost.ageRange,
                        JPAExpressions.select(qImage.imageUrl.min().coalesce("https://danimdata.s3.ap-northeast-2.amazonaws.com/Frame+2448+(2).png"))
                                .from(qImage)
                                .where(qImage.post.id.eq(qPost.id))
                                .orderBy(qImage.id.asc()),
                        qPost.gender,
                        qPost.isRecruitmentEnd,
                        qPost.member.imageUrl))
                .from(qPost)
                .where(qPost.isDeleted.eq(false))
                .orderBy(qPost.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }
    // 게시글 상세 조회

    @Transactional
    public ResponseEntity<Message> readPost(Long id) {

        Post post = postRepository.findById(id)
            .orElseThrow(()->new CustomException(ErrorCode.POST_NOT_FOUND));

        List<MemberChatRoom> memberChatRoomList = memberChatRoomRepository.findAllByChatRoom_Id(post.getChatRoom().getId());
        List<Long> participants = new ArrayList<>();
        for(MemberChatRoom memberChatRoom : memberChatRoomList) {
            Long memberId = memberChatRoom.getMember().getId();
            participants.add(memberId);
        }
      
        PostResponseDto postResponseDto = new PostResponseDto(post, participants);

        Message message = Message.setSuccess(StatusEnum.OK, "게시글 단일 조회 성공", postResponseDto);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    // @Transactional
    // public ResponseEntity<Message> readPost(Long id) throws JsonProcessingException {
    //     PostResponseDto postResponseDto  = cacheService.postRes(id);
    //     Message message = Message.setSuccess(StatusEnum.OK, "게시글 단일 조회 성공", postResponseDto);
    //     return new ResponseEntity<>(message, HttpStatus.OK);
    // }


}
