package com.project.danim_be.review.entity;

import com.project.danim_be.member.entity.Member;
import com.project.danim_be.post.entity.Post;
import com.project.danim_be.review.dto.ReviewRequestDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Nested
@ExtendWith(MockitoExtension.class)
class ReviewTest {

    @Mock
    private Post post;
    @Mock
    private Member member;

    @Test
    @DisplayName("Review 엔티티 생성 테스트")
    void createMemberTest() {

        // given
        String comment = "너무 즐거웠어요!";
        Double score = 5.0;

        ReviewRequestDto reviewRequestDto = new ReviewRequestDto(comment, score);
        Review review = new Review(reviewRequestDto, post, member);

        // when, then
        Assertions.assertEquals(5.0, review.getPoint());
        Assertions.assertEquals("너무 즐거웠어요!", review.getComment());

    }
}