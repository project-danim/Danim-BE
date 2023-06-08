package com.project.danim_be.member.dto;

import com.project.danim_be.member.entity.Member;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MypageResponseDto {

    private String nickname;
    private String imageUrl;
    private String content;
    private Boolean owner;
    private Double score;

    public MypageResponseDto(Member member, Boolean owner) {
        this.nickname = member.getNickname();
        this.imageUrl = member.getImageUrl();
        this.content = member.getContent();
        this.owner = owner;
        this.score = member.getScore();
    }
}
