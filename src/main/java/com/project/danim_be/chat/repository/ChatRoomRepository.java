package com.project.danim_be.chat.repository;


import com.project.danim_be.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, QuerydslPredicateExecutor<ChatRoom> {
	Optional<ChatRoom> findByRoomName(String roomName);

	// Optional<ChatRoom> findByChatMessage(ChatMessage chatMessage);

	ChatRoom findByChatMessagesId(Long id);
}
