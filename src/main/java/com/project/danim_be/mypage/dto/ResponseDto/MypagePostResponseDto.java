package com.project.danim_be.mypage.dto.ResponseDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.danim_be.post.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MypagePostResponseDto {

    private Long id;
    private String postTitle;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date tripEndDate;
    private String content;
    private String imageUrl;
    private Boolean owner;

    public MypagePostResponseDto(Post post, Boolean owner) {
        this.id = post.getId();
        this.postTitle = post.getPostTitle();
        this.tripEndDate = post.getTripEndDate();
        this.content = post.getContent();
        if (!post.getImageUrls().isEmpty()) {
            this.imageUrl = post.getImageUrls().get(0).getImageUrl();
        } else {
            this.imageUrl = "https://danimdata.s3.ap-northeast-2.amazonaws.com/basicImage.png";}
        this.owner = owner;

    }
}
