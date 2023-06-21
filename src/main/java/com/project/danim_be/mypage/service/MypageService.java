package com.project.danim_be.mypage.service;

import com.project.danim_be.common.exception.CustomException;
import com.project.danim_be.common.exception.ErrorCode;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.common.util.S3Uploader;
import com.project.danim_be.common.util.StatusEnum;
import com.project.danim_be.member.entity.Member;
import com.project.danim_be.member.entity.QMember;
import com.project.danim_be.member.repository.MemberRepository;
import com.project.danim_be.mypage.dto.RequestDto.MypageRequestDto;
import com.project.danim_be.mypage.dto.ResponseDto.MypagePostResponseDto;
import com.project.danim_be.mypage.dto.ResponseDto.MypageResponseDto;
import com.project.danim_be.mypage.dto.ResponseDto.MypageReviewResponseDto;
import com.project.danim_be.post.entity.QImage;
import com.project.danim_be.post.entity.QPost;
import com.project.danim_be.review.entity.QReview;
import com.project.danim_be.review.entity.Review;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.project.danim_be.common.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class MypageService {

    private final MemberRepository memberRepository;
    private final S3Uploader s3Uploader;
    private final JPAQueryFactory queryFactory;


    //마이페이지 - 사용자 정보
    @Transactional(readOnly = true)
    public ResponseEntity<Message> memberInfo(Long ownerId, Long memberId) {
        Member owner = memberRepository.findById(ownerId).orElseThrow(
                () -> new CustomException(USER_NOT_FOUND)
        );
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new CustomException(USER_NOT_FOUND)
        );
        MypageResponseDto mypageResponseDto;
        if (ownerId.equals(memberId)){
            mypageResponseDto = new MypageResponseDto(member, true);
        } else {
            mypageResponseDto = new MypageResponseDto(owner, false);
        }
        return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "조회 성공", mypageResponseDto));
    }

    //마이페이지 게시물 정보
    @Transactional(readOnly = true)
    public ResponseEntity<Message> memberPosts(Long ownerId, Long memberId) {

        if (!findMember(ownerId) || !findMember(memberId)){
            throw new CustomException(USER_NOT_FOUND);
        }
        List<MypagePostResponseDto> mypagePostResponseDtoList;
        if (ownerId.equals(memberId)) {
            mypagePostResponseDtoList = validMember(memberId, true);
        } else {
            mypagePostResponseDtoList = validMember(ownerId, false);
        }
        return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "조회 성공", mypagePostResponseDtoList));
    }

    //마이페이지 내가 받은 후기
    @Transactional(readOnly = true)
    public ResponseEntity<Message> memberReview(Long ownerId, Long memberId) {

        if (!findMember(ownerId) || !findMember(memberId)){
            throw new CustomException(USER_NOT_FOUND);
        }
        List<MypageReviewResponseDto> reviewList;

        if (ownerId.equals(memberId)){
            reviewList = getReview(memberId);
        } else {
            reviewList = getReview(ownerId);
        }
        return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "조회 성공", reviewList));
    }

    //마이페이지 회원정보 수정
   @Transactional
    public ResponseEntity<Message> editMember(Long ownerId, MypageRequestDto mypageRequestDto, Member member) throws IOException {

        if (ownerId.equals(member.getId())) {

                String imageUrl = s3Uploader.upload(mypageRequestDto.getImage());
                member.editMember(mypageRequestDto, imageUrl);

                memberRepository.save(member);
            }
         else throw new CustomException(ErrorCode.DO_NOT_HAVE_PERMISSION);
        return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "수정 완료"));

    }

    //마이페이지 게시물 공통 메서드
    private List<MypagePostResponseDto> validMember(Long memberId, Boolean owner) {
//        List<Post> postList = postRepository.findAllByMemberOrderByCreatedAtDesc(member);
        QPost qPost = QPost.post;
        QImage qImage = QImage.image;
        QMember qMember = QMember.member;

//        List<MypagePostResponseDto> mypagePostResponseDtoList = new ArrayList<>();
//        for (Post post : postList) {
//            mypagePostResponseDtoList.add(new MypagePostResponseDto(post, owner));
//        }
        return queryFactory.
                select(Projections.constructor(MypagePostResponseDto.class,
                        qPost.id,
                        qPost.postTitle,
                        qPost.tripEndDate,
                        qPost.content,
                        JPAExpressions.select(qImage.imageUrl.min().coalesce("https://danimdata.s3.ap-northeast-2.amazonaws.com/Frame+2448+(2).png"))
                                .from(qImage)
                                .where(qImage.post.id.eq(qPost.id))
                                .orderBy(qImage.id.asc()),
                        Expressions.asBoolean(owner).as("owner")))
                .from(qPost)
                .where(qPost.member.id.eq(memberId))
                .orderBy(qPost.createdAt.desc())
                .fetch();
    }

    //멤버 검증 공통 메서드
    private Boolean findMember(Long id) {
        return memberRepository.existsById(id);
    }

    //마이페이지 리뷰 공통 메서드
    private List<MypageReviewResponseDto> getReview(Long memberId) {
        QReview qReview = QReview.review;
        QPost qPost = QPost.post;

//        return queryFactory
//                .select(Projections.constructor(MypageReviewResponseDto.class,
//                        qReview.member.nickname,
//                        qReview.point,
//                        qReview.comment,
//                        qReview.createdAt))
//                .join(qReview.post, qPost)
//                .where(qPost.member.id.eq(memberId))
//                .fetch();

        List<Review> reviewList = queryFactory
                .selectFrom(qReview)
                .join(qReview.post, qPost).fetchJoin()
                .where(qPost.member.id.eq(memberId))
                .fetch();

        List<MypageReviewResponseDto> mypageReviewResponseDtoList = new ArrayList<>();
        for (Review review : reviewList) {
            mypageReviewResponseDtoList.add(new MypageReviewResponseDto(review));
        }
        return mypageReviewResponseDtoList;
    }
}
