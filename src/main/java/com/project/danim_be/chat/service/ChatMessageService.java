package com.project.danim_be.chat.service;


import com.project.danim_be.chat.dto.ChatDto;
import com.project.danim_be.chat.entity.ChatMessage;
import com.project.danim_be.chat.entity.ChatRoom;
import com.project.danim_be.chat.entity.MemberChatRoom;
import com.project.danim_be.chat.repository.ChatMessageRepository;
import com.project.danim_be.chat.repository.ChatRoomRepository;
import com.project.danim_be.chat.repository.MemberChatRoomRepository;
import com.project.danim_be.common.exception.CustomException;
import com.project.danim_be.common.exception.ErrorCode;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.common.util.StatusEnum;
import com.project.danim_be.member.entity.Member;
import com.project.danim_be.member.repository.MemberRepository;
import com.project.danim_be.notification.service.NotificationService;
import com.project.danim_be.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final MemberChatRoomRepository memberChatRoomRepository;
	private final MemberRepository memberRepository;
	private final PostRepository postRepository;
	private final NotificationService notificationService;


	//채팅방 입장멤버 저장메서드	ENTER
	@Transactional
	public void visitMember(ChatDto chatDto){
		String roomId = chatDto.getRoomId();
		String sender = chatDto.getSender();
		//sender(nickName)을 통해서 멤버를찾고
		Member member = memberRepository.findByNickname(sender)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		//roomId를 통해서 생성된 채팅룸을 찾고
		ChatRoom chatRoom= chatRoomRepository.findByRoomId(roomId)
			.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
		//MemberChatRoom 에 멤버와 챗룸 연결되어있는지 찾는다
		MemberChatRoom memberChatRoom = memberChatRoomRepository.findByMemberAndChatRoom(member, chatRoom).orElse(null);
		//강퇴당한사람인지 검사한다.
		if (memberChatRoom != null) {
			if (memberChatRoom.getKickMember()) {
				throw new CustomException(ErrorCode.USER_KICKED);
			} else {
				List<ChatDto> previousMessages = allChatList(chatDto);
			}
		}
		//첫 연결시도이면
		if(isFirstVisit(member.getId(),roomId)){

			memberChatRoom = new MemberChatRoom(member, chatRoom);
			memberChatRoom.setFirstJoinRoom(LocalDateTime.now());	//맨처음 연결한시간과
		}else{
			if(memberChatRoom==null){
				throw new CustomException(ErrorCode.FAIL_FIND_MEMBER_CHAT_ROOM);
			}
		}
		memberChatRoom.setRecentConnect(LocalDateTime.now());  //최근 접속한 시간
		memberChatRoomRepository.save(memberChatRoom);
		// return previousMessages;
	}
	//메시지저장  TALK
	@Transactional
	public void sendMessage(ChatDto chatDto) {
		String roomId = chatDto.getRoomId();

		ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
			.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
		Member sendMember = memberRepository.findByNickname(chatDto.getSender())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		if(!sendMember.getNickname().equals(chatDto.getSender())){
			throw new CustomException(ErrorCode.SENDER_MISMATCH);
		}

		//강퇴당한사람인지 검사한다.

		MemberChatRoom memberChatRoom = memberChatRoomRepository.findByMemberAndChatRoom(sendMember, chatRoom)
			.orElseThrow(()->new CustomException(ErrorCode.ROOM_NOT_FOUND));
// 		if ( memberChatRoom.getKickMember()) {
// 			throw new CustomException(ErrorCode.USER_KICKED);
// 		}


		// 메세지를 보낸사람. 이 사람에겐 알람을 안보내기 위해
		List<MemberChatRoom> memberChatRoomList = memberChatRoomRepository.findByChatRoom(chatRoom);
		List<Member> members = new ArrayList<>();
		for (MemberChatRoom memberChatroom : memberChatRoomList) {
			members.add(memberChatroom.getMember());
		}
		List<Long> memberIdlist = new ArrayList<>();
		for (Member member : members) {
			memberIdlist.add(member.getId());
		}
		memberIdlist.remove(sendMember.getId());
		ChatMessage chatMessage= new ChatMessage(chatDto,chatRoom);
		notificationService.send(memberIdlist, chatMessage.getId(), memberChatRoom.getId());
		chatMessageRepository.save(chatMessage);
	}
	//방을 나갔는지확인해야함 	LEAVE
	@Transactional
	public void leaveChatRoom(ChatDto chatDto) {
		String roomId = chatDto.getRoomId();
		String sender = chatDto.getSender();

		Member member = memberRepository.findByNickname(sender)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		ChatRoom chatRoom= chatRoomRepository.findByRoomId(roomId)
			.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
		MemberChatRoom memberChatRoom = memberChatRoomRepository.findByMemberAndChatRoom(member, chatRoom)
			.orElseThrow(() -> new CustomException(ErrorCode.FAIL_FIND_MEMBER_CHAT_ROOM));
		memberChatRoom.setRecentDisConnect(LocalDateTime.now());

		memberChatRoomRepository.save(memberChatRoom);
	}
	//강퇴기능	KICK
	@Transactional
	public void kickMember(ChatDto chatDto) {

		ChatRoom chatRoom = chatRoomRepository.findByRoomId(chatDto.getRoomId())
			.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
		//방장
		Member superMember = memberRepository.findById(chatRoom.getAdminMemberId())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		//강퇴를 요청하는 멤버
		Member member = memberRepository.findByNickname(chatDto.getSender())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		//강퇴당하는 임포스터
		Member imposter = memberRepository.findByNickname(chatDto.getImposter())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		if (member.equals(superMember)) {
			MemberChatRoom  memberChatRoomImposter = memberChatRoomRepository.findByMemberAndChatRoom(imposter, chatRoom)
				.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

			memberChatRoomImposter.setKickMember(true);
			memberChatRoomRepository.save(memberChatRoomImposter);
			//참여자를 한명 추가해야하는 로직이필요합니다.
		} else {
			throw new CustomException(ErrorCode.NOT_ADMIN_ACCESS);
		}

	}
	//메시지조회
	@Transactional(readOnly = true)
	public ResponseEntity<Message> chatList(ChatDto chatDto){
		String roomId = chatDto.getRoomId();
		String nickName= chatDto.getSender();

		Member member = memberRepository.findByNickname(nickName)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		if (chatDto.getSender().equals(member.getNickname()) && isFirstVisit(member.getId(),roomId)){
			List<ChatDto> allChats = allChatList(chatDto);
			Message message = Message.setSuccess(StatusEnum.OK,"게시글 작성 성공");
			return new ResponseEntity<>(message, HttpStatus.OK);
		}


		Message message = Message.setSuccess(StatusEnum.OK,"게시글 작성 성공");
		return new ResponseEntity<>(message, HttpStatus.OK);
	}
	//첫방문 확인
	private boolean isFirstVisit(Long memberId, String roomId){
		return !memberChatRoomRepository.existsByMember_IdAndChatRoom_RoomId(memberId, roomId);
		//xistsBy 메소드는 특정 조건을 만족하는 데이터가 존재하는지를 검사하고
		// 그 결과를 boolean으로 반환
	}
	//채팅메시지 목록 보여주기
	private List<ChatDto> allChatList(ChatDto chatDto){
		String roomId = chatDto.getRoomId();
		ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
			.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

		List<ChatMessage> chatList = chatMessageRepository.findAllByChatRoom(chatRoom);

		// Convert ChatMessage list to ChatDto list
		List<ChatDto> chatDtoList = chatList.stream()
			.map(chatMessage -> ChatDto.builder()
				.type(chatMessage.getType())
				.roomId(chatMessage.getChatRoom().getRoomId())
				.sender(chatMessage.getSender())
				.message(chatMessage.getMessage())
				.build())
			.collect(Collectors.toList());

		return chatDtoList;

	}


}
