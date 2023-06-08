package com.project.danim_be.chat.entity;

import com.project.danim_be.post.entity.Post;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String roomName;					//방번호
	private Long adminMemberId;

	@OneToMany(mappedBy = "chatRoom")
	private List<MemberChatRoom> memberChatRoomList;

	@OneToOne
	@JoinColumn(name = "post_id")
	private Post post;

	@OneToMany
	private List<ChatMessage> chatMessages;


}
