package com.project.danim_be.member.entity;

import com.project.danim_be.common.entity.Timestamped;
import com.project.danim_be.member.dto.MypageRequestDto;
import com.project.danim_be.member.dto.UserInfoRequestDto;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Member extends Timestamped {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String userId;		//아이디

	@Column(nullable = false)
	private String password;	//비밀번호

	private String nickname;	//닉네임

	private String ageRange;	//연령대

	private Boolean agreeForAge;

	private String provider;	//소셜

	private String imageUrl;	//프로필이미지

	private String gender;		//성별

	private Boolean agreeForGender;

	private String content;		//(간략한)자기소개

	private Boolean isDeleted;		//탈퇴 여부

	private Double score;		//점수

	public Member(String userId, String password, String nickname) {
		this.userId = userId;
		this.password = password;
		this.nickname = nickname;
		this.ageRange = "don't receive";
		this.provider = "GOOGLE";
  }

	@Builder
	public Member(String userId, String gender, String password, String nickname, String ageRange, String provider, Boolean isDeleted, Boolean agreeForAge, Boolean agreeForGender, Double score) {
		this.userId = userId;
		this.gender = gender;
		this.password = password;
		this.nickname = nickname;
		this.ageRange = ageRange;
		this.provider = provider;
		this.isDeleted = isDeleted;
		this.agreeForGender = agreeForGender;
		this.agreeForAge = agreeForAge;
		this.score = score;
	}

	public void editMember (MypageRequestDto mypageRequestDto,String imageUrl) {
		this.imageUrl = imageUrl;
		this.content = mypageRequestDto.getContent();
		this.nickname = mypageRequestDto.getNickname();
	}


	public String getUserId() {
		return userId;
	}

	public void signOut() {
		this.isDeleted = true;
		this.userId = this.userId + "(withdrawal)";
		this.nickname = this.nickname + "(withdrawal)";
	}

	public void update(UserInfoRequestDto userInfoRequestDto) {
		this.gender = userInfoRequestDto.getGender();
		this.ageRange = userInfoRequestDto.getAgeRange();
		this.agreeForGender = userInfoRequestDto.isAgreeForGender();
		this.agreeForAge = userInfoRequestDto.isAgreeForAge();
	}
}
