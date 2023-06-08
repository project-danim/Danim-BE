package com.project.danim_be.chat.repository;

import com.project.danim_be.chat.entity.ChatRoom;
import com.project.danim_be.chat.entity.MemberChatRoom;
import com.project.danim_be.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberChatRoomRepository extends JpaRepository<MemberChatRoom,Long> {
	boolean existsByMember_IdAndChatRoom_RoomName(Long memberId, String roomName);
	boolean existsByMember_IdAndChatRoom_Id(Long memberId, Long roomId);
	Optional<MemberChatRoom> findByMemberAndChatRoom(Member member, ChatRoom chatRoom);
	List<MemberChatRoom> findByChatRoom(ChatRoom chatRoom);
	Optional<MemberChatRoom> findByMember(Member member);
	List<MemberChatRoom> findAllByChatRoom_Id(Long id);

}
