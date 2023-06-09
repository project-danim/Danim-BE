package com.project.danim_be.chat.service;

import com.project.danim_be.chat.config.SubscribeCheck;
import com.project.danim_be.chat.dto.ChatDto;
import com.project.danim_be.chat.entity.ChatMessage;
import com.project.danim_be.chat.entity.ChatRoom;
import com.project.danim_be.chat.entity.MemberChatRoom;
import com.project.danim_be.chat.repository.ChatMessageRepository;
import com.project.danim_be.chat.repository.ChatRoomRepository;
import com.project.danim_be.chat.repository.MemberChatRoomRepository;
import com.project.danim_be.common.exception.CustomException;
import com.project.danim_be.common.exception.ErrorCode;
import com.project.danim_be.member.entity.Member;
import com.project.danim_be.member.repository.MemberRepository;
import com.project.danim_be.post.entity.Post;
import com.project.danim_be.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageService {

	@EventListener
	public void handleSubscriptionEvent(SubscribeCheck.SubscriptionEvent event) {
		alarmList(event.getUserId());
	}

	@Autowired
	private SimpMessageSendingOperations messagingTemplate;

	@Autowired
	private RedisTemplate<String, Object> chatRedisTemplate;

	private final MemberChatRoomRepository memberChatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final MemberRepository memberRepository;
	private final PostRepository postRepository;

	//채팅방 입장멤버 저장메서드 ENTER
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
		Post post = postRepository.findByChatRoom_Id(chatRoom.getId()).orElseThrow(
				() -> new CustomException(ErrorCode.POST_NOT_FOUND)
		);

		//MemberChatRoom 에 멤버와 챗룸 연결되어있는지 찾는다
		MemberChatRoom memberChatRoom = memberChatRoomRepository.findByMemberAndChatRoom(member, chatRoom).orElse(null);

		//첫 연결시도이면
		if(isFirstVisit(member.getId(),roomName)) {
			memberChatRoom = new MemberChatRoom(member, chatRoom);
			memberChatRoom.setFirstJoinRoom(LocalDateTime.now());
			if(!chatRoom.getAdminMemberId().equals(member.getId())&&!post.getId().equals(55L)) {
				post.incNumberOfParticipants();
			}
			postRepository.save(post);

		} else {
			if(memberChatRoom==null) {
				throw new CustomException(ErrorCode.FAIL_FIND_MEMBER_CHAT_ROOM);
			}
		}

		memberChatRoom.setRecentConnect(LocalDateTime.now());

		ChatDto message = ChatDto.builder()
				.type(ChatDto.MessageType.ENTER)
				.roomName(chatDto.getRoomName())
				.sender(chatDto.getSender())
				.time(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
				.message(isFirstVisit(member.getId(),roomName) ? chatDto.getSender() + "님이 입장하셨습니다." : "")
				.build();

		if (isFirstVisit(member.getId(),roomName)){
			ChatMessage chatMessage = new ChatMessage(message, chatRoom);
			chatMessageRepository.save(chatMessage);
		}

		memberChatRoomRepository.save(memberChatRoom);

		//alarm 초기화
		memberChatRoom.InitializationAlarm (0);
		alarmList(member.getId());

		//킥멤버리스트보내주기
		imposters(chatDto,chatRoom);

		return message;

	}

	@Transactional
	public void imposters(ChatDto chatDto,ChatRoom chatRoom){

		List<MemberChatRoom> memberChatRoomList = memberChatRoomRepository.findAllByChatRoom_Id(chatRoom.getId());
		Map<String,List<String>>imposters =new HashMap<>();
		List<String>imposter=new ArrayList<>();

		for(MemberChatRoom memberChatRoom : memberChatRoomList) {
			if(memberChatRoom.getKickMember()) {
				imposter.add(memberChatRoom.getMember().getNickname());
			}
			imposters.put("imposters",imposter);
		}

		messagingTemplate.convertAndSend("/sub/chat/room/" + chatDto.getRoomName(), imposters);

	}

	//메시지저장  TALK
	@Transactional
	public void sendMessage(ChatDto chatDto) {

		String roomName = chatDto.getRoomName();

		ChatRoom chatRoom =chatRoomRepository.findByRoomName(roomName)
				.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

		Member sendMember = memberRepository.findByNickname(chatDto.getSender())
				.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		List<Long> memberIdList = memberChatRoomRepository.findByChatRoom(chatRoom).stream()
				.map(MemberChatRoom::getMember)
				.map(Member::getId)
				.filter(id -> !id.equals(sendMember.getId()))
				.toList();

		increaseAlarm(memberIdList,chatRoom);

		ChatMessage chatMessage = new ChatMessage(chatDto, chatRoom);
		chatMessageRepository.save(chatMessage);
		// 레디스저장
		// chatRedisTemplate.opsForList().rightPush(roomName, chatMessage);
	}

	@Transactional
	public void alarmList(Long memberId) {

		List<MemberChatRoom> memberChatRoomList = memberChatRoomRepository.findAllByMember_Id(memberId);
		List<Map<String,Integer>> alarm = new ArrayList<>();

		int sum=0;

		for(MemberChatRoom memberChatRoom : memberChatRoomList) {
			Map<String, Integer> result = new HashMap<>();
			result.put(memberChatRoom.getChatRoom().getId().toString(), memberChatRoom.getAlarm());
			sum += memberChatRoom.getAlarm();
			alarm.add(result);
		}

		Map<String, Integer> sumMap = new HashMap<>();
		sumMap.put("sum", sum);
		alarm.add(sumMap);
		messagingTemplate.convertAndSend("/sub/alarm/" + memberId, alarm);

	}

	@Transactional
	public void increaseAlarm(List<Long> memberIdList, ChatRoom chatRoom) {

		for (Long memberId : memberIdList) {
			MemberChatRoom memberChatRoom = memberChatRoomRepository.findByMemberIdAndChatRoom(memberId, chatRoom)
					.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
			if (memberChatRoom.getRecentDisConnect()!=null && memberChatRoom.getRecentDisConnect().isAfter(memberChatRoom.getRecentConnect())) {
				memberChatRoom.increaseAlarm (1);
				memberChatRoomRepository.save(memberChatRoom);
				if(memberChatRoom.getAlarm()>0) {
					alarmList(memberId);
				}
			}
		}
	}
	// 10분마다 저장
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

	//강퇴기능 KICK
	@Transactional
	public void kickMember(ChatDto chatDto) {

		ChatRoom chatRoom = chatRoomRepository.findByRoomName(chatDto.getRoomName())
				.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

		// chatRoom.getAdminMemberId() == chatDto.getSender()
		Member superMember = memberRepository.findByNickname(chatDto.getSender())
				.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		Member kickedMember = memberRepository.findByNickname(chatDto.getImposter())
				.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		Post post = postRepository.findById(chatRoom.getPost().getId())
				.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

		if(chatDto.getSender().equals(chatDto.getImposter())){
			throw new CustomException(ErrorCode.CANNOT_KICK);
		}

		if (superMember.getId().equals(chatRoom.getAdminMemberId())) {
			MemberChatRoom  memberChatRoomImposter = memberChatRoomRepository.findByMemberAndChatRoom(kickedMember, chatRoom)
					.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

			post.decNumberOfParticipants();
			memberChatRoomImposter.setKickMember(true);
			memberChatRoomRepository.save(memberChatRoomImposter);

			ChatMessage chatMessage = new ChatMessage(chatDto, chatRoom);
			chatMessage.setMessage(chatDto.getSender() + "님이 " + chatDto.getImposter() + "을(를) 강퇴하였습니다.");
			chatMessageRepository.save(chatMessage);
			Map<String,String> imposter = new HashMap<>();
			imposter.put("imposter",chatDto.getImposter());
			// messagingTemplate.convertAndSend("/sub/chat/room/" + chatDto.getRoomName(), imposter);

		} else {
			throw new CustomException(ErrorCode.NOT_ADMIN_ACCESS);
		}

	}

	//첫방문 확인
	private boolean isFirstVisit(Long memberId, String roomName){

		return !memberChatRoomRepository.existsByMember_IdAndChatRoom_RoomName(memberId, roomName);

	}

}
