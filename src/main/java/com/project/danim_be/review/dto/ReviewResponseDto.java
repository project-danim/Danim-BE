package com.project.danim_be.review.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.danim_be.review.entity.Review;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
public class ReviewResponseDto {

    private Long id;
    private String userImageUrl;
    private String nickname;
    private String comment;
    private Double score;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    public ReviewResponseDto(Review review) {
        this.id = review.getId();
        this.userImageUrl = review.getMember().getImageUrl();
        this.nickname = review.getMember().getNickname();
        this.comment = review.getComment();
        this.score = review.getPoint();
        this.createdAt = review.getCreatedAt();
    }
}
