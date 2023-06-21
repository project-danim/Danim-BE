package com.project.danim_be.review.entity;

import com.project.danim_be.common.entity.Timestamped;
import com.project.danim_be.member.entity.Member;
import com.project.danim_be.post.entity.Post;
import com.project.danim_be.review.dto.ReviewRequestDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Review extends Timestamped {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Double point;		//점수

	@Column(nullable = false)
	private String comment;		//후기

	@ManyToOne(fetch = FetchType.LAZY)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id")
	private Post post;

	public Review(ReviewRequestDto reviewRequestDto, Post post, Member member) {
		this.comment = reviewRequestDto.getComment();
		this.point = reviewRequestDto.getScore();
		this.post = post;
		this.member = member;
	}
}
