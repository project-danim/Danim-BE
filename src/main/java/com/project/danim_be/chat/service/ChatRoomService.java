package com.project.danim_be.chat.service;

import com.project.danim_be.chat.dto.ChatRoomDto;
import com.project.danim_be.chat.dto.ChatRoomResponseDto;
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
import com.project.danim_be.post.entity.Post;
import com.project.danim_be.post.repository.PostRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

	private final MemberChatRoomRepository memberChatRoomRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final PostRepository postRepository;
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
		return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK,"내가 만든 채팅방",chatRoomResponseDtoList));
	}

	//내가 신청한 채팅방 목록조회
	public ResponseEntity<Message> myJoinChatroom(Long id) {
		QMemberChatRoom qMemberChatRoom = QMemberChatRoom.memberChatRoom;
		List<ChatRoom> chatRoomList = queryFactory
				.select(qMemberChatRoom.chatRoom)
				.from(qMemberChatRoom)
				.where(qMemberChatRoom.member.id.eq(id))
				.fetch();
		List<ChatRoomResponseDto> chatRoomResponseDtoList = new ArrayList<>();
		for (ChatRoom chatroom : chatRoomList) {
			if (!chatroom.getPost().getMember().getId().equals(id)){
				chatRoomResponseDtoList.add(new ChatRoomResponseDto(chatroom));
			}
		}
		return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK,"내가 참여한 채팅방", chatRoomResponseDtoList)); // 쿼리문 짜기
	}

	//채팅방 참여(웹소켓연결/방입장) == 매칭 신청 버튼
	@Transactional
	public ResponseEntity<Message> joinChatRoom(Long id, Member member) {
		ChatRoom chatRoom = chatRoomRepository.findById(id).orElseThrow(
				() -> new CustomException(ErrorCode.ROOM_NOT_FOUND)
		);

		Post post = postRepository.findByChatRoom_Id(id);

		Date recruitmentEndDate = post.getRecruitmentEndDate();
		// LocalDate 타입으로 변환
		LocalDate localDate = new java.sql.Date(recruitmentEndDate.getTime()).toLocalDate();
		LocalDate today = LocalDate.now();

		// 현재 날짜가 모집 종료일보다 늦다면 true
		boolean afterDate = today.isAfter(localDate);
		// 모집이 종료되면
		if(afterDate) throw new CustomException(ErrorCode.EXPIRED_RECRUIT);

		// 모집 인원이 다 차기 전까지 신청 가능
		if(post.getNumberOfParticipants() < post.getGroupSize()) {
			// 채팅방 입장 시 모든 유저 nickname 보내주기
			List<MemberChatRoom> memberChatRoomList = memberChatRoomRepository.findAllByChatRoom_Id(id);
			List<ChatRoomDto> chatRoomDtoList = new ArrayList<>();
			for (MemberChatRoom memberChatRoom : memberChatRoomList) {
				chatRoomDtoList.add(new ChatRoomDto(memberChatRoom));
			}

			Post validPost = postRepository.findById(id).orElseThrow(
					() -> new CustomException(ErrorCode.POST_NOT_FOUND)
			);

			String ageRange = validPost.getAgeRange().toString();
			String[] ageRangeArray = ageRange.split(",");
			String gender = validPost.getGender().toString();
			String[] genderArray = gender.split(",");
			if (Arrays.asList(ageRangeArray).contains(member.getGender()) && Arrays.asList(genderArray).contains(member.getGender())) {
				// 작성자가 아니고?? 방에 처음 들어온다면 참여인원 +1
				if (!memberChatRoomRepository.existsByMember_IdAndChatRoom_RoomId(member.getId(), chatRoom.getRoomId())) {
					post.incNumberOfParticipants();
					postRepository.save(post);
				}
			} else throw new CustomException(ErrorCode.NOT_MATCHING);

			return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "채팅방 입장", chatRoomDtoList));
		} else {
			throw new CustomException(ErrorCode.COMPLETE_MATCHING);
		}
	}
}
