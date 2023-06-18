package com.project.danim_be.chat.controller;

import com.project.danim_be.chat.dto.ChatDto;
import com.project.danim_be.chat.dto.test.RoomIdRequestDto;
import com.project.danim_be.chat.service.ChatMessageService;
import com.project.danim_be.chat.service.ChatRoomService;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.security.auth.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;

import java.time.ZoneId;


@Controller
@RequiredArgsConstructor
public class ChatController {

	@Autowired
	private SimpMessageSendingOperations messagingTemplate;
	private final ChatMessageService chatMessageService;
	private final ChatRoomService chatRoomService;

	// 메시지가왔을때 실행
	@MessageMapping("/chat/message")
	public void message(@Payload ChatDto chatDto, Principal principal) throws Exception {

		switch (chatDto.getType()) {

			case ENTER -> {
				System.out.println("TYPE : ENTER");

				chatMessageService.visitMember(chatDto);

				ChatDto message = ChatDto.builder()
						.type(ChatDto.MessageType.ENTER)
						.roomName(chatDto.getRoomName())
						.sender(chatDto.getSender())
						.time(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
						.message(chatDto.getSender() + "님이 입장하셨습니다.")
						.build();
				messagingTemplate.convertAndSend("/sub/chat/room/" + chatDto.getRoomName(), message);
			}

			case TALK -> {
				System.out.println("TYPE : TALK");

				chatMessageService.sendMessage(chatDto);

				ChatDto message = ChatDto.builder()
						.type(ChatDto.MessageType.TALK)
						.roomName(chatDto.getRoomName())
						.sender(chatDto.getSender())
						.message(chatDto.getMessage())
						.time(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
						.build();

				messagingTemplate.convertAndSend("/sub/chat/room/" + chatDto.getRoomName(), message);
			}

			case LEAVE -> {
				System.out.println("TYPE : LEAVE");
				chatMessageService.leaveChatRoom(chatDto);
				//SSE요청시작!
				ChatDto leaveMessage = ChatDto.builder()
						.type(ChatDto.MessageType.LEAVE)
						.roomName(chatDto.getRoomName())
						.sender(chatDto.getSender())
						.time(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
						.message(chatDto.getSender() + "님이 접속을 끊었습니다.")
						.build();

				messagingTemplate.convertAndSend("/sub/chat/room/" + chatDto.getRoomName(), leaveMessage);
			}
			case KICK -> {
				System.out.println("TYPE : KICK");
				chatMessageService.kickMember(chatDto);

				ChatDto kickMessage = ChatDto.builder()
						.type(ChatDto.MessageType.KICK)
						.roomName(chatDto.getRoomName())
						.sender(chatDto.getSender())
						.imposter(chatDto.getImposter())
						.time(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
						.message(chatDto.getSender() + "님이 " + chatDto.getImposter() + "을(를) 강퇴하였습니다.")
						.build();
				messagingTemplate.convertAndSend("/sub/chat/room/" + chatDto.getRoomName(), kickMessage);
			}
		}
	}

	//		@PostMapping("api/chat/{roomId}")
	//		public ResponseEntity<Message> joinChatRoom (@PathVariable("Post_id") Long id, @AuthenticationPrincipal UserDetailsImpl userDetails){
	//			return chatRoomService.joinChatRoom(id, userDetails.getMember());
	//		}
	// 	채팅방 참여(웹소켓연결/방입장)
	//	매칭 신청
	//	버튼
	@PostMapping("/api/chat/room/{roomId}")
	public ResponseEntity<Message> joinChatRoom(@PathVariable Long roomId,
												@AuthenticationPrincipal UserDetailsImpl userDetails) {
		return chatRoomService.joinChatRoom(roomId, userDetails.getMember());
	}

	//내가 쓴글의 채팅방 목록조회
	// @GetMapping("/api/chat/myChatRoom")
	// public ResponseEntity<Message> myChatRoom(@AuthenticationPrincipal UserDetailsImpl userDetails) {
	// 	return chatRoomService.myChatRoom(userDetails.getMember().getId());
	// }

	//=========================================================테스트용 메서드(완료시 삭제)===================================
	@GetMapping("/api/chat/allChatRoom")
	public ResponseEntity<Message> allChatRoom() {
		return chatRoomService.allChatRoom();
	}

	@PostMapping("/api/chat/test")
	public ResponseEntity<Message> chatTestMember(@RequestBody RoomIdRequestDto roomIdRequestDto) {

		return chatRoomService.roomMember(roomIdRequestDto);
	}

	//신청취소(나가기)
	@DeleteMapping("/api/chat/room/{roomId}")
	public ResponseEntity<Message> leaveChatRoom(@PathVariable Long roomId, @AuthenticationPrincipal UserDetailsImpl userDetails) {
		return chatRoomService.leaveChatRoom(roomId, userDetails.getMember());
	}
}
//=========================================================테스트용 메서드(완료시 삭제)===================================
 // 	//내가 신청한 채팅방 목록조회
 // 	@GetMapping("/api/chat/joinChatRoom")
 // 	public ResponseEntity<Message> myJoinChatroom(@AuthenticationPrincipal UserDetailsImpl userDetails) {
 // 		return chatRoomService.myJoinChatroom(userDetails.getMember().getId());
 // 	}
 // }



	//추방하기



	//=================================================================================================================================


