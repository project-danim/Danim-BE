package com.project.danim_be.review.service;

import com.project.danim_be.chat.repository.MemberChatRoomRepository;
import com.project.danim_be.common.exception.CustomException;
import com.project.danim_be.common.exception.ErrorCode;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.common.util.StatusEnum;
import com.project.danim_be.member.entity.Member;
import com.project.danim_be.member.repository.MemberRepository;
import com.project.danim_be.post.entity.Post;
import com.project.danim_be.post.repository.PostRepository;
import com.project.danim_be.review.dto.ReviewRequestDto;
import com.project.danim_be.review.dto.ReviewResponseDto;
import com.project.danim_be.review.entity.Review;
import com.project.danim_be.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final MemberChatRoomRepository memberChatRoomRepository;

    // 리뷰 작성
    @Transactional
    public ResponseEntity<Message> createReview(Long postId, ReviewRequestDto reviewRequestDto, Member member) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 작성 여부 체크
        if(!reviewRepository.existsByMember_IdAndPost_Id(member.getId(), postId)){

            Date tripEndDate = post.getTripEndDate();

            // LocalDate 타입으로 변환
            LocalDate localDate = new java.sql.Date(tripEndDate.getTime()).toLocalDate();
            LocalDate today = LocalDate.now();

            // 현재 날짜가 여행 종료일보다 늦다면 true
            boolean afterDate = today.isAfter(localDate);

            // 여행에 참여한 사람만 작성 가능
            if(memberChatRoomRepository.existsByMember_IdAndChatRoom_Id(member.getId(),post.getChatRoom().getId())) {

                //Mile계산식
                Member planner = memberRepository.findById(post.getMember().getId())
                        .orElseThrow(()->new CustomException(ErrorCode.USER_NOT_FOUND));

                Double score = reviewRequestDto.getScore();
                score -= 3;
                score = score/(post.getNumberOfParticipants());
                score *= (10+post.getNumberOfParticipants())/10.0;
                score += planner.getScore();
                planner.setScore(score);

                // 여행이 종료된 후에 작성 가능 (테스트기간 작동 X)
//                if(afterDate){
                    Review review = new Review(reviewRequestDto, post, member);
                    reviewRepository.save(review);
                    return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "리뷰 작성 완료"));
//                } else {
//                    throw new CustomException(ErrorCode.CANNOT_WRITE_REVIEW);
//                }
            } else {
                throw new CustomException(ErrorCode.NOT_WRITE_MEMBER);
            }
        } else {
            throw new CustomException(ErrorCode.ALREADY_WRITTEN);
        }

    }

    // 댓글 조회
    @Transactional(readOnly = true)
    public ResponseEntity<Message> readReview(Long postId) {

        List<Review> reviewList = reviewRepository.findAllByPostId(postId);
        List<ReviewResponseDto> reviewResponseDtoList = new ArrayList<>();

        for (Review review : reviewList) {
            reviewResponseDtoList.add(new ReviewResponseDto(review));
        }
        return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "조회 성공", reviewResponseDtoList));
    }
}
