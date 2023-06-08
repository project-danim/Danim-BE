package com.project.danim_be.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.danim_be.post.entity.Post;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class PostResponseDto {


	private Long 		postId;					//게시글번호
	private String		nickName;				//닉네임
	private String 		postTitle;				//게시글제목
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date recruitmentStartDate;			//모집 시작날짜
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date recruitmentEndDate;			//모집 마감날짜
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date tripStartDate;					//여행 시작날짜
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date tripEndDate;					//여행 마감날짜
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdAt;			//게시글 작성시간
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime modifiedAt;			//게시글 수정시간
	private List<String> ageRange;				//연령대
	private List<String> gender;				//연령대
	private String  keyword;					//키워드
	private int groupSize;						//인원수
	private String location;					//지역
	private String content;						//게시글
	private String map;							//지도 api 정보
	private Long chatRoomId;					//채팅방아이디



	public PostResponseDto(Post post){
		this.postId = post.getId();
		this.nickName = post.getMember().getNickname();
		this.postTitle = post.getPostTitle();
		this.recruitmentStartDate=post.getRecruitmentStartDate();
		this.recruitmentEndDate=post.getRecruitmentEndDate();
		this.tripStartDate = post.getTripStartDate();
		this.tripEndDate = post.getTripEndDate();
		this.location = post.getLocation();
		this.groupSize = post.getGroupSize();
		this.keyword = post.getKeyword();
		this.ageRange = post.getAgeRange();
		this.gender = post.getGender();
		this.content = post.getContent().getContent();
		this.map = post.getMap().getMap();
		this.chatRoomId = post.getChatRoom().getId();
		this.createdAt = post.getCreatedAt();
		this.modifiedAt = post.getModifiedAt();
	}
}
