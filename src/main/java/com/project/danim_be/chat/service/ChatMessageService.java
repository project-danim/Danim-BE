package com.project.danim_be.chat.service;


import com.project.danim_be.chat.dto.ChatDto;
import com.project.danim_be.chat.entity.ChatMessage;
import com.project.danim_be.chat.entity.ChatRoom;
import com.project.danim_be.chat.entity.MemberChatRoom;
import com.project.danim_be.chat.repository.ChatMessageRepository;
import com.project.danim_be.chat.repository.ChatRoomRepository;
import com.project.danim_be.chat.repository.MemberChatRoomRepository;
import com.project.danim_be.common.CacheService;
import com.project.danim_be.common.exception.CustomException;
import com.project.danim_be.common.exception.ErrorCode;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.common.util.StatusEnum;
import com.project.danim_be.member.entity.Member;
import com.project.danim_be.member.repository.MemberRepository;
import com.project.danim_be.notification.service.NotificationService;
import com.project.danim_be.post.entity.Post;
import com.project.danim_be.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageService {

	@Autowired
	private RedisTemplate<String, Object> chatRedisTemplate;

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final MemberChatRoomRepository memberChatRoomRepository;
	private final MemberRepository memberRepository;
	private final PostRepository postRepository;
	private final NotificationService notificationService;
	private final ChatRoomService chatRoomService;
	private final CacheService cacheService;


	//채팅방 입장멤버 저장메서드	ENTER
	@Transactional
	public ChatDto visitMember(ChatDto chatDto) {

		String roomName = chatDto.getRoomName();
		String sender = chatDto.getSender();

		//sender(nickName)을 통해서 멤버를찾고
		Member member = memberRepository.findByNickname(sender)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		//roomId를 통해서 생성된 채팅룸을 찾고
		ChatRoom chatRoom= chatRoomRepository.findByRoomName(roomName)
			.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
		Post post = postRepository.findByChatRoom_Id(chatRoom.getId()).get();

		//MemberChatRoom 에 멤버와 챗룸 연결되어있는지 찾는다
		MemberChatRoom memberChatRoom = memberChatRoomRepository.findByMemberAndChatRoom(member, chatRoom).orElse(null);


		//첫 연결시도이면
		if(isFirstVisit(member.getId(),roomName)){
			memberChatRoom = new MemberChatRoom(member, chatRoom);
			memberChatRoom.setFirstJoinRoom(LocalDateTime.now());	//맨처음 연결한시간과
			if(!chatRoom.getAdminMemberId().equals(member.getId())) {
				post.incNumberOfParticipants();
			}
				postRepository.save(post);

		}else{
			if(memberChatRoom==null){
				throw new CustomException(ErrorCode.FAIL_FIND_MEMBER_CHAT_ROOM);
			}
		}
		memberChatRoom.setRecentConnect(LocalDateTime.now());  //최근 접속한 시간
		

		ChatDto message = ChatDto.builder()
			.type(ChatDto.MessageType.ENTER)
			.roomName(chatDto.getRoomName())
			.sender(chatDto.getSender())
			.time(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
			.message(isFirstVisit(member.getId(),roomName) ? chatDto.getSender() + "님이 입장하셨습니다." : "")
			.build();

		memberChatRoomRepository.save(memberChatRoom);
		return message;

	}
	//메시지저장  TALK
	@Transactional
	public void sendMessage(ChatDto chatDto) {

		String roomName = chatDto.getRoomName();
		//
		ChatRoom chatRoom =chatRoomRepository.findByRoomName(roomName)
			.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
		//
		Member sendMember = memberRepository.findByNickname(chatDto.getSender())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		// if(!sendMember.getNickname().equals(chatDto.getSender())){
		// 	throw new CustomException(ErrorCode.SENDER_MISMATCH);
		// }

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
		// chatRedisTemplate.opsForList().rightPush("chatMessages", chatMessage);
	}
	// // 10분마다 저장
	// @Scheduled(fixedDelay = 600_000)
	// public void saveMessages() {
	// 	List<Object> chatMessages = chatRedisTemplate.opsForList().range("chatMessages", 0, -1);
	// 	System.out.println("저장");
	// 	chatRedisTemplate.opsForList().trim("chatMessages", 1, 0);
	// 	for (Object chatMessage : chatMessages) {
	// 		ChatMessage cm = (ChatMessage) chatMessage;
	// 		ChatRoom chatRoom = chatRoomRepository.findByRoomName(cm.getChatRoomName())
	// 			.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
	// 		cm.setChatRoom(chatRoom);
	//
	// 		chatMessageRepository.save(cm);
	// 	}
	// }
	//방을 나갔는지확인해야함	LEAVE
	@Transactional
	public void leaveChatRoom(ChatDto chatDto) {
		String roomName = chatDto.getRoomName();
		String sender = chatDto.getSender();

		Member member = memberRepository.findByNickname(sender)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		ChatRoom chatRoom= chatRoomRepository.findByRoomName(roomName)
			.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

		MemberChatRoom memberChatRoom = memberChatRoomRepository.findByMemberAndChatRoom(member, chatRoom)
			.orElseThrow(() -> new CustomException(ErrorCode.FAIL_FIND_MEMBER_CHAT_ROOM));

		memberChatRoom.setRecentDisConnect(LocalDateTime.now());

		memberChatRoomRepository.save(memberChatRoom);
	}
	//강퇴기능	KICK
	@Transactional
	public void kickMember(ChatDto chatDto) {

		ChatRoom chatRoom = chatRoomRepository.findByRoomName(chatDto.getRoomName())
			.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
		//방장
		String sen=chatDto.getSender();

		// chatRoom.getAdminMemberId() == chatDto.getSender()
		Member superMember = memberRepository.findByNickname(sen)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		Member member1 = memberRepository.findById(chatRoom.getAdminMemberId())
			.orElseThrow(() ->new CustomException(ErrorCode.USER_NOT_FOUND));

		Post post = postRepository.findById(chatRoom.getPost().getId())
			.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
		// Member member2 = memberRepository.findById(post.getMember().getId())
		// 	.orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));
		// System.out.println(member2.getNickname());
		//강퇴당하는 임포스터
		Member imposter = memberRepository.findByNickname(chatDto.getImposter())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		if (superMember.getId().equals(chatRoom.getAdminMemberId())) {
			MemberChatRoom  memberChatRoomImposter = memberChatRoomRepository.findByMemberAndChatRoom(imposter, chatRoom)
				.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

			post.decNumberOfParticipants();
			memberChatRoomImposter.setKickMember(true);
			memberChatRoomRepository.save(memberChatRoomImposter);

		} else {
			throw new CustomException(ErrorCode.NOT_ADMIN_ACCESS);
		}

	}
	//메시지조회
	@Transactional(readOnly = true)
	public ResponseEntity<Message> chatList(ChatDto chatDto){

		ChatRoom chatRoom= chatRoomRepository.findByRoomName(chatDto.getRoomName()).get();

		Member member = memberRepository.findByNickname(chatDto.getSender())
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));


		List<ChatMessage> chatList = chatMessageRepository.findAllByChatRoom(chatRoom);


		Message message = Message.setSuccess(StatusEnum.OK,"게시글 작성 성공");
		return new ResponseEntity<>(message, HttpStatus.OK);
	}
	//첫방문 확인
	private boolean isFirstVisit(Long memberId, String roomName){
		return !memberChatRoomRepository.existsByMember_IdAndChatRoom_RoomName(memberId, roomName);
		//xistsBy 메소드는 특정 조건을 만족하는 데이터가 존재하는지를 검사하고
		//그 결과를 boolean으로 반환
	}
	//채팅메시지 목록 보여주기
	private List<ChatDto> allChatList(ChatDto chatDto){

		ChatRoom chatRoom = chatRoomRepository.findByRoomName(chatDto.getRoomName())
			.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

		List<ChatMessage> chatList = chatMessageRepository.findAllByChatRoom(chatRoom);

		// Convert ChatMessage list to ChatDto list
		List<ChatDto> chatDtoList = chatList.stream()
			.map(chatMessage -> ChatDto.builder()
				.type(chatMessage.getType())
				.roomName(chatMessage.getChatRoom().getRoomName())
				.sender(chatMessage.getSender())
				.message(chatMessage.getMessage())
				.build())
			.collect(Collectors.toList());

		return chatDtoList;

	}



}
