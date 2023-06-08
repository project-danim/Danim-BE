package com.project.danim_be.post.service;


import com.project.danim_be.chat.entity.ChatRoom;
import com.project.danim_be.chat.repository.ChatRoomRepository;
import com.project.danim_be.common.exception.CustomException;
import com.project.danim_be.common.exception.ErrorCode;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.common.util.S3Uploader;
import com.project.danim_be.common.util.StatusEnum;
import com.project.danim_be.member.entity.Member;
import com.project.danim_be.post.dto.ImageRequestDto;
import com.project.danim_be.post.dto.PostRequestDto;
import com.project.danim_be.post.dto.PostResponseDto;
import com.project.danim_be.post.entity.Content;
import com.project.danim_be.post.entity.Image;
import com.project.danim_be.post.entity.MapApi;
import com.project.danim_be.post.entity.Post;
import com.project.danim_be.post.repository.ContentRepository;
import com.project.danim_be.post.repository.ImageRepository;
import com.project.danim_be.post.repository.MapApiRepository;
import com.project.danim_be.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

	private final PostRepository postRepository;
	private final ContentRepository contentRepository;
	private final ImageRepository imageRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final MapApiRepository mapApiRepository;


	private final S3Uploader s3Uploader;

	private static final Logger logger = LoggerFactory.getLogger(PostService.class);

	//게시글작성
	@Transactional
	public ResponseEntity<Message> createPost(Member member, PostRequestDto requestDto) {
		// logger.info("Received PostRequestDto: {}", requestDto.toString());
		// logger.info("Received PostRequestDto: {}", requestDto.getContents().toString());

		Post post = Post.builder()
			.postTitle(requestDto.getPostTitle())
			.recruitmentStartDate(requestDto.getRecruitmentStartDate())
			.recruitmentEndDate(requestDto.getRecruitmentEndDate())
			.tripStartDate(requestDto.getTripStartDate())
			.tripEndDate(requestDto.getTripEndDate())
			.location(requestDto.getLocation())
			.groupSize(requestDto.getGroupSize())
			.keyword(requestDto.getKeyword())
			.ageRange(String.join(",", requestDto.getAgeRange()))
			.gender(String.join(",", requestDto.getGender()))
			.numberOfParticipants(0)
			.member(member)
			.isDeleted(false)
			.build();



		Content content = Content.builder()
			.post(post)
			.content(requestDto.getContent())
			.build();
		contentRepository.save(content);
    
		MapApi map = MapApi.builder()
			.post(post)
			.map(requestDto.getMapAPI())
			.build();
		mapApiRepository.save(map);

		for(String url : requestDto.getImageUrls()) {
			Image image = Image.builder()
				.post(post)
				.imageUrl(url)
				.build();
			imageRepository.save(image);
		}



		String roomName = UUID.randomUUID().toString();
		ChatRoom chatRoom =ChatRoom.builder()
			.roomName(roomName)
			.post(post)
			.adminMemberId(post.getMember().getId())
			.build();
		post.setChatRoom(chatRoom);
		chatRoomRepository.save(chatRoom);

		postRepository.save(post);

		// PostResponseDto postResponseDto = new PostResponseDto(post);
		Message message = Message.setSuccess(StatusEnum.OK,"게시글 작성 성공");
		return new ResponseEntity<>(message, HttpStatus.OK);
	}
	//이미지 업로드
	@Transactional
	public ResponseEntity<Message> imageUpload(ImageRequestDto requestDto) {
		MultipartFile imageFile = requestDto.getImage();

		String imageUrl = uploader(imageFile);

		Image image = new Image(imageUrl);
		imageRepository.save(image);


		Message message = Message.setSuccess(StatusEnum.OK, "이미지 업로드 성공",imageUrl);
		return new ResponseEntity<>(message, HttpStatus.OK);

	}
	//게시글 수정
	@Transactional
	public ResponseEntity<Message> updatePost(Long id,Member member, PostRequestDto requestDto) {
		Post post = postRepository.findById(id).orElseThrow(()
			->new CustomException(ErrorCode.POST_NOT_FOUND));

		if (!post.getMember().getId().equals(member.getId())) {
			throw new CustomException(ErrorCode.NOT_MOD_AUTHORIZED_MEMBER);
		}

		post.update(requestDto);

		Content content = contentRepository.findByPostId(id)
			.orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
		content.update(requestDto.getContent());
		contentRepository.save(content);

		MapApi map = mapApiRepository.findByPostId(id)
			.orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
		map.update(requestDto.getMapAPI());
		mapApiRepository.save(map);

		//보류
		for(String url : requestDto.getImageUrls()) {
			Image image = Image.builder()
				.post(post)
				.imageUrl(url)
				.build();
			imageRepository.save(image);
		}

		Message message = Message.setSuccess(StatusEnum.OK, "게시글 수정 성공");
		return new ResponseEntity<>(message, HttpStatus.OK);
	}
	//게시글 삭제
	@Transactional
	public ResponseEntity<Message> deletePost(Long id,Member member) {
		
		Post post = postRepository.findById(id).orElseThrow(()
			->new CustomException(ErrorCode.POST_NOT_FOUND));

		if (!post.getMember().getId().equals(member.getId())) {
			throw new CustomException(ErrorCode.NOT_DEL_AUTHORIZED_MEMBER);
		}
		
		post.delete();

		Message message = Message.setSuccess(StatusEnum.OK, "게시글 삭제 성공");
		return new ResponseEntity<>(message, HttpStatus.OK);
	}

	public String uploader(MultipartFile imageFile){
		String file = null;
		try {
			file = s3Uploader.upload(imageFile);
		} catch (IOException e) {
			throw new CustomException(ErrorCode.FILE_CONVERT_FAIL);
		}
		return file;
	}




}
