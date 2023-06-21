package com.project.danim_be.mypage.dto.ResponseDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.danim_be.review.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MypageReviewResponseDto {

    private String nickName;
    private Double point;
    private String comment;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    public MypageReviewResponseDto(Review receiveReview) {
        this.nickName = receiveReview.getMember().getNickname();
        this.point = receiveReview.getPoint();
        this.comment = receiveReview.getComment();
        this.createdAt = receiveReview.getCreatedAt();
    }
}
