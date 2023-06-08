package com.project.danim_be.chat.service;

import com.project.danim_be.chat.dto.ChatDto;
import com.project.danim_be.chat.dto.ChatRoomDto;
import com.project.danim_be.chat.dto.ChatRoomResponseDto;
import com.project.danim_be.chat.dto.test.ChatRoomIdDto;
import com.project.danim_be.chat.dto.test.ChatRoomMemberInfoDto;
import com.project.danim_be.chat.dto.test.RoomIdRequestDto;
import com.project.danim_be.chat.entity.ChatRoom;
import com.project.danim_be.chat.entity.MemberChatRoom;
import com.project.danim_be.chat.entity.QMemberChatRoom;
import com.project.danim_be.chat.repository.ChatRoomRepository;
import com.project.danim_be.chat.repository.MemberChatRoomRepository;
import com.project.danim_be.common.exception.CustomException;
import com.project.danim_be.common.exception.ErrorCode;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.common.util.StatusEnum;
import com.project.danim_be.member.entity.Member;
import com.project.danim_be.member.repository.MemberRepository;
import com.project.danim_be.post.entity.Post;
import com.project.danim_be.post.repository.PostRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

	private final MemberChatRoomRepository memberChatRoomRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final PostRepository postRepository;
	private final MemberRepository memberRepository;
	private final JPAQueryFactory queryFactory;

	//내가 쓴글의 채팅방 목록조회
	public ResponseEntity<Message> myChatRoom(Long id) {
		List<Post> postList = postRepository.findByMember_Id(id);
		List<ChatRoomResponseDto> chatRoomResponseDtoList = new ArrayList<>();
		for (Post post : postList) {
			ChatRoom chatRoom = post.getChatRoom();
			ChatRoomResponseDto chatRoomResponseDto = new ChatRoomResponseDto(chatRoom);
			chatRoomResponseDtoList.add(chatRoomResponseDto);
		}
		return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "내가 만든 채팅방", chatRoomResponseDtoList));
	}
	//======================================================테스트용 메서드=========================================//
	public ResponseEntity<Message> allChatRoom() {
		List <ChatRoom> chatRooms = chatRoomRepository.findAll();
		List<ChatRoomIdDto> chatRoomIdDtos = new ArrayList<>();
		for(ChatRoom c : chatRooms){
			String roomId = c.getRoomName();
			ChatRoomIdDto dto = new ChatRoomIdDto(roomId);
			chatRoomIdDtos.add(dto);
		}
		return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "모든채팅방", chatRoomIdDtos));
	}

	public ResponseEntity<Message> roomMember(RoomIdRequestDto roomIdRequestDto) {
		log.info(roomIdRequestDto.getRoomId());
		ChatRoom chatRoom =chatRoomRepository.findByRoomName(roomIdRequestDto.getRoomId())
			.orElseThrow(()->new CustomException(ErrorCode.ROOM_NOT_FOUND));
		System.out.println("chatRoom : "+chatRoom.getId());
		List<MemberChatRoom> memberChatRoomList = memberChatRoomRepository.findAllByChatRoom_Id(chatRoom.getId());
		List<ChatRoomMemberInfoDto> chatRoomMemberInfoDto = new ArrayList<>();
		for(MemberChatRoom chatRoomId :  memberChatRoomList) {
			// Suppose ChatRoomMemberInfoDto has a constructor that takes a MemberChatRoom
			ChatRoomMemberInfoDto infoDto = new ChatRoomMemberInfoDto(chatRoomId);
			chatRoomMemberInfoDto.add(infoDto);
		}
		return  ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "채팅방유저 목록조회완료", chatRoomMemberInfoDto));
		}
	//=========================================테스트용 메서드===================================================//
	// 내가 신청한 채팅방 목록조회
	public ResponseEntity<Message> myJoinChatroom(Long id) {
		QMemberChatRoom qMemberChatRoom = QMemberChatRoom.memberChatRoom;
		List<ChatRoom> chatRoomList = queryFactory
				.select(qMemberChatRoom.chatRoom)
				.from(qMemberChatRoom)
				.where(qMemberChatRoom.member.id.eq(id))
				.fetch();
		List<ChatRoomResponseDto> chatRoomResponseDtoList = new ArrayList<>();
		for (ChatRoom chatroom : chatRoomList) {
			if (!chatroom.getPost().getMember().getId().equals(id)) {
				chatRoomResponseDtoList.add(new ChatRoomResponseDto(chatroom));
			}
		}
		return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "내가 참여한 채팅방", chatRoomResponseDtoList)); // 쿼리문 짜기
	}

	//채팅방 참여(웹소켓연결/방입장) == 매칭 신청 버튼
	@Transactional
	public ResponseEntity<Message> joinChatRoom(Long id, Member member) {

		ChatRoom chatRoom = chatRoomRepository.findById(id)
				.orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
		//방을찾고
		Post post = postRepository.findByChatRoom_Id(id)
				.orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
		//삭제된 게시글
		if(post.getIsDeleted().equals(true)) {
			throw new CustomException(ErrorCode.POST_NOT_FOUND);
		}
		//신청한유저를찾고
		Member subscriber = memberRepository.findById(member.getId())
				.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

		//연령대 조건 비교하고
		if (!chatRoom.getAdminMemberId().equals(member.getId()) && !post.getAgeRange().contains(subscriber.getAgeRange())) {
			throw new CustomException(ErrorCode.USER_KICKED);
		}
		System.out.println(post.getGender());
		System.out.println(subscriber.getGender());
		//성별 조건 비교하고
		if (!chatRoom.getAdminMemberId().equals(member.getId()) &&!post.getGender().contains(subscriber.getGender())) {
			throw new CustomException(ErrorCode.NOT_CONTAIN_GENDER);
		}

		Date recruitmentEndDate = post.getRecruitmentEndDate();
		// LocalDate 타입으로 변환
		LocalDate localDate = new java.sql.Date(recruitmentEndDate.getTime()).toLocalDate();
		LocalDate today = LocalDate.now();

		// 현재 날짜가 모집 종료일보다 늦다면 true
		boolean afterDate = today.isAfter(localDate);
		// 모집이 종료되면
		if (afterDate) throw new CustomException(ErrorCode.EXPIRED_RECRUIT);
		MemberChatRoom memberChatRooms = memberChatRoomRepository.findByMemberAndChatRoom(member, chatRoom).orElse(null);
		if (memberChatRooms != null) {
			System.out.println("안됨");
			if (memberChatRooms.getKickMember()) {
				throw new CustomException(ErrorCode.USER_KICKED);
			}
		}
		// 모집 인원이 다 차기 전까지 신청 가능
		Map<String, Object> chatRoomDtoList = new HashMap<>();
		chatRoomDtoList.put("roomName",chatRoom.getRoomName());
		if (post.getNumberOfParticipants() < post.getGroupSize()) {
			// 작성자가 아니고?? 방에 처음 들어온다면 참여인원 +1
			if (!memberChatRoomRepository.existsByMember_IdAndChatRoom_RoomName(member.getId(), chatRoom.getRoomName())) {
				post.incNumberOfParticipants();
				postRepository.save(post);

				// 채팅방 입장 시 모든 유저 nickname 보내주기
				List<MemberChatRoom> memberChatRoomList = memberChatRoomRepository.findAllByChatRoom_Id(id);
				List<String> nickNames = new ArrayList<>();
				for (MemberChatRoom memberChatRoom : memberChatRoomList) {
					nickNames.add(memberChatRoom.getMember().getNickname());
				}
				chatRoomDtoList.put("nickName", nickNames);
			}
		} else {
			throw new CustomException(ErrorCode.COMPLETE_MATCHING);
		}

		return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "모임 신청 완료", chatRoomDtoList));
	}


}