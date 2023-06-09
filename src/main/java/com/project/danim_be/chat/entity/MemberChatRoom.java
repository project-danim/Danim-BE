package com.project.danim_be.chat.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.project.danim_be.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class MemberChatRoom implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	private Member member;

	@ManyToOne
	@JsonBackReference
	private ChatRoom chatRoom;

	private LocalDateTime firstJoinRoom;	// 맨처음 채팅방에 들어온시간
	private LocalDateTime recentConnect;	// 최근 접속한시간
	private LocalDateTime recentDisConnect;	// 마지막으로 떠난시간 (채팅방 접속을끊은시간)(강퇴.신청취소아님)
	private Boolean kickMember = false;		// 방에서 강퇴당한 이력이 있는지 true:강퇴당함
	private int alarm;

	//setter
	public void setFirstJoinRoom(LocalDateTime now) {
		this.firstJoinRoom = now;
	}

	public void setRecentConnect(LocalDateTime now) {
		this.recentConnect = now;
	}

	public void setRecentDisConnect(LocalDateTime now) {
		this.recentDisConnect = now;
	}

	public MemberChatRoom(Member member, ChatRoom chatRoom) {
		this.member = member;
		this.chatRoom = chatRoom;
	}

	//강퇴
	public void setKickMember(boolean b) {
		this.kickMember = b;
	}

	//알람수증가
	public void increaseAlarm (int i) {
		this.alarm += i;
	}

	//알람초기화
	public void InitializationAlarm(int i) {
		this.alarm = i;
	}

}
