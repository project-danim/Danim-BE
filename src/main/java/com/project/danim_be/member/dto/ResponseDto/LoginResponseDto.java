package com.project.danim_be.member.dto.ResponseDto;

import com.project.danim_be.member.entity.Member;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Getter
@NoArgsConstructor
public class LoginResponseDto {
	private Long id;
	private SseEmitter sseEmitter;
	private String nickName;
	private String myPageImageUrl;
	private Boolean isExistMember;

	public LoginResponseDto(Member member, SseEmitter sseEmitter, Boolean isExistMember) {
		this.id = member.getId();
		this.nickName = member.getNickname();
		this.myPageImageUrl = member.getImageUrl();
		this.sseEmitter = sseEmitter;
		this.isExistMember = isExistMember;
	}

	public LoginResponseDto(Member member, SseEmitter sseEmitter) {
		this.id = member.getId();
		this.nickName = member.getNickname();
		this.myPageImageUrl = member.getImageUrl();
		this.sseEmitter = sseEmitter;
	}

}
