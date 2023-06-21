package com.project.danim_be.post.dto.ResponseDto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.danim_be.post.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

@Getter
@AllArgsConstructor
public class CardPostResponseDto {

    private Long id;
    private String postTitle;
    @JsonFormat(pattern="yyyy-MM-dd", timezone = "Asia/Seoul")
    private Date recruitmentEndDate;
    private String nickname;
    private Integer numberOfParticipants;
    private Integer groupSize;
    private String location;
    private String keyword;
    private String ageRange;
    private String imageUrl;
    private String gender;
    private Boolean isRecruitmentEnd;
    private String userImage;

//    public CardPostResponseDto(Post post) {
//        this.id = post.getId();
//        this.postTitle = post.getPostTitle();
//        this.recruitmentEndDate = post.getRecruitmentEndDate();
//        if (!post.getImageUrls().isEmpty()) {
//            this.imageUrl = post.getImageUrls().get(0).getImageUrl();
//        } else {
//            this.imageUrl = "https://danimdata.s3.ap-northeast-2.amazonaws.com/Frame+2448+(2).png";}
//        this.nickname = post.getMember().getNickname();
//        this.numberOfParticipants = post.getNumberOfParticipants();
//        this.groupSize = post.getGroupSize();
//        this.location = post.getLocation();
//        this.keyword = post.getKeyword();
//        this.ageRange = String.join(",", post.getAgeRange());
//        this.gender = String.join(",", post.getGender());
//        this.isRecruitmentEnd = post.getIsRecruitmentEnd();
//        this.userImage = post.getMember().getImageUrl();
//    }

}
