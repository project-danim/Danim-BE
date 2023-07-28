package com.project.danim_be.member.service;

import com.project.danim_be.chat.entity.MemberChatRoom;
import com.project.danim_be.chat.repository.MemberChatRoomRepository;
import com.project.danim_be.common.exception.CustomException;
import com.project.danim_be.common.exception.ErrorCode;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.common.util.RandomNickname;
import com.project.danim_be.common.util.StatusEnum;
import com.project.danim_be.member.dto.RequestDto.*;
import com.project.danim_be.member.dto.ResponseDto.LoginResponseDto;
import com.project.danim_be.member.entity.Member;
import com.project.danim_be.member.repository.MemberRepository;
import com.project.danim_be.post.entity.Post;
import com.project.danim_be.post.repository.PostRepository;
import com.project.danim_be.security.jwt.JwtUtil;
import com.project.danim_be.security.jwt.TokenDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static com.project.danim_be.common.exception.ErrorCode.FAIL_SIGNOUT;
import static com.project.danim_be.common.exception.ErrorCode.USER_NOT_FOUND;
import static com.project.danim_be.security.jwt.JwtUtil.REFRESH_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final PostRepository postRepository;
	private final MemberChatRoomRepository memberChatRoomRepository;
	private final JwtUtil jwtUtil;
	private final SocialService socialService;
	private final RandomNickname randomNickname;
	private final RedisTemplate<String, String> RefreshTokenRedisTemplate;

	//회원가입
	@Transactional
	public ResponseEntity<Message> signup(SignupRequestDto signupRequestDto) {

		String userId = signupRequestDto.getUserId();
		String password = passwordEncoder.encode(signupRequestDto.getPassword());
		String nickname = signupRequestDto.getNickname();
		String ageRange = signupRequestDto.getAgeRange();
		String gender = signupRequestDto.getGender();

		Boolean agreeForGender = signupRequestDto.getAgreeForGender();
		Boolean agreeForAge = signupRequestDto.getAgreeForAge();

		if(memberRepository.findByUserId(userId).isPresent()) {
			throw new CustomException(ErrorCode.DUPLICATE_IDENTIFIER);
		}
		if(memberRepository.findByNickname(nickname).isPresent()) {
			throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
		}

		Member member = Member.builder()
				.userId(userId)
				.ageRange(ageRange)
				.gender(gender)
				.nickname(nickname)
				.password(password)
				.agreeForGender(agreeForGender)
				.agreeForAge(agreeForAge)
				.provider("DANIM")
				.isDeleted(false)
				.score(20.0)
				.imageUrl("https://danimdata.s3.ap-northeast-2.amazonaws.com/avatar.png")
				.build();

		memberRepository.save(member);

		Message message = Message.setSuccess(StatusEnum.OK,"회원 가입 성공");
		return new ResponseEntity<>(message, HttpStatus.OK);

	}

	//일반 회원가입 아이디 중복 검사
	public ResponseEntity<Message> checkId(CheckIdRequestDto checkIdRequestDto) {

		if(memberRepository.findByUserId(checkIdRequestDto.getUserId()).isPresent()) {
			throw new CustomException(ErrorCode.DUPLICATE_IDENTIFIER);
		} else {
			Message message = Message.setSuccess(StatusEnum.OK,"아이디 중복 검사 성공");
			return new ResponseEntity<>(message, HttpStatus.OK);
		}

	}

	//일반 회원가입 닉네임 중복 검사
	public ResponseEntity<Message> checkNickname(CheckNicknameRequestDto checkNicknameRequestDto) {

		if(memberRepository.findByNickname(checkNicknameRequestDto.getNickname()).isPresent()) {
			throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
		} else {
			Message message = Message.setSuccess(StatusEnum.OK,"닉네임 중복 검사 성공");
			return new ResponseEntity<>(message, HttpStatus.OK);
		}

  }

	//랜덤 닉네임 생성
	@Transactional
	public ResponseEntity<Message> nicknameCreate() {

		String nickname = randomNickname.getRandomNickname();
		return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "랜덤 닉네임 생성완료", nickname));

	}

	//소셜 로그인 시 추가 회원 정보 작성
	@Transactional
	public ResponseEntity<Message> addUserInfo(UserInfoRequestDto userInfoRequestDto) {

		Member member = memberRepository.findById(userInfoRequestDto.getUserId()).orElseThrow(
				() -> new CustomException(ErrorCode.ID_NOT_FOUND)
		);

		member.update(userInfoRequestDto);
		TokenDto tokenDto = jwtUtil.createAllToken(member.getUserId());

		LoginResponseDto loginResponseDto = new LoginResponseDto(member, tokenDto.getAccessToken(), tokenDto.getRefreshToken());
		return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "로그인 성공", loginResponseDto));

	}

	// 로그인
	private static final Queue<LoginRequestDto> loginQueue = new LinkedList<>();
	private static final Object lock = new Object();

	@Transactional
	public ResponseEntity<Message> login(LoginRequestDto requestDto, HttpServletResponse response) {

		String userId = requestDto.getUserId();
		String password = requestDto.getPassword();

		Member member;

		synchronized (lock) {
			loginQueue.add(requestDto);

			if(loginQueue.peek() != requestDto) {
				// 대기 큐에 추가된 요청이 현재 요청이 아니라면 대기 상태로 진입
				while(loginQueue.peek() != requestDto) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			// 현재 요청이 처리될 차례이므로 대기 큐에서 제거
			loginQueue.remove();
			lock.notifyAll();

			member = memberRepository.findByUserId(userId).orElseThrow(() -> new CustomException(ErrorCode.ID_NOT_FOUND));
		}

		if(!passwordEncoder.matches(password, member.getPassword())) {
			throw new CustomException(ErrorCode.INVALID_PASSWORD);
		}

		TokenDto tokenDto = jwtUtil.createAllToken(userId);

		RefreshTokenRedisTemplate.opsForValue().set(
				userId,
				tokenDto.getRefreshToken().substring(7),
				14,
				TimeUnit.DAYS);

//		setHeader(response, tokenDto);


//		[For HttpOnly]
//		Cookie accessTokenCookie = new Cookie("ACCESS_KEY", tokenDto.getAccessToken());
//		accessTokenCookie.setHttpOnly(true);
//		accessTokenCookie.setSecure(true);
//		response.addCookie(accessTokenCookie);
//		ResponseCookie responseCookie = ResponseCookie.from("REFRESH_KEY", tokenDto.getRefreshToken().substring(7))
//				.path("/")
//				.sameSite("None")
//				.httpOnly(true)
//				.secure(true)
//				.domain("www.da-nim.com")
//				.build();

		String refreshToken = tokenDto.getRefreshToken();
		if(refreshToken.startsWith("Bearer ")){
			refreshToken = refreshToken.substring(7);
		}

//		Cookie refreshTokenCookie = new Cookie("REFRESH_KEY", tokenDto.getRefreshToken().substring(7));
		Cookie refreshTokenCookie = new Cookie(REFRESH_KEY, refreshToken);
		refreshTokenCookie.setHttpOnly(true);
//		refreshTokenCookie.setSecure(true);
		refreshTokenCookie.setPath("/");
		refreshTokenCookie.setDomain("www.da-nim.com");
		response.addCookie(refreshTokenCookie);





		LoginResponseDto loginResponseDto = new LoginResponseDto(member, tokenDto.getAccessToken(), tokenDto.getRefreshToken());
		Message message = Message.setSuccess(StatusEnum.OK, "로그인 성공", loginResponseDto);

		return new ResponseEntity<>(message, HttpStatus.OK);
	}

	//로그아웃
	@Transactional
	public ResponseEntity<Message> logout(Member member, HttpServletRequest request) {

//		String accessToken = request.getHeader("ACCESS_KEY").substring(7);
		if(RefreshTokenRedisTemplate.opsForValue().get(member.getUserId()) != null ) {
//			Long tokenTime = jwtUtil.getExpirationTime(accessToken);
			RefreshTokenRedisTemplate.delete(member.getUserId());
//			refreshTokenRepository.deleteByUserIdAndProvider(member.getUserId(), "DANIM");
			Message message = Message.setSuccess(StatusEnum.OK,"로그아웃 성공", member.getUserId());
			return new ResponseEntity<>(message, HttpStatus.OK);
		}
		throw new CustomException(USER_NOT_FOUND);

	}

	//회원 탈퇴
	@Transactional
	public ResponseEntity<Message> signOut(Member member) {

		member = memberRepository.findById(member.getId()).orElseThrow(() -> new CustomException(USER_NOT_FOUND));

		List<Post> posts = postRepository.findByMember_Id(member.getId());

		//채팅방입장삭제
		List<MemberChatRoom> memberChatRooms = memberChatRoomRepository.findAllByMember_Id(member.getId());
		memberChatRoomRepository.deleteAll(memberChatRooms);

		//게시물삭제
		for(Post post : posts) {
			post.decNumberOfParticipants();
			post.delete();
		}

		if(!member.getProvider().equals("DANIM")) {
			try {
				socialService.socailSignout(member);
				RefreshTokenRedisTemplate.delete(member.getUserId());
			} catch (IOException e) {
				throw new CustomException(FAIL_SIGNOUT);
			}
		} else {
			try {
				RefreshTokenRedisTemplate.delete(member.getUserId());
			} catch (IncorrectResultSizeDataAccessException e) {
				throw new CustomException(FAIL_SIGNOUT);
			}
		}

		member.signOut();
		return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "탈퇴 성공"));

	}

	//헤더 셋팅
	private void setHeader(HttpServletResponse response, TokenDto tokenDto) {

		response.addHeader(JwtUtil.ACCESS_KEY, tokenDto.getAccessToken());
		response.addHeader(REFRESH_KEY, tokenDto.getRefreshToken());

	}

	@Transactional
	public ResponseEntity<Message> refreshAccessToken(HttpServletRequest httpServletRequest, HttpServletResponse response) {

		String refresh_token = jwtUtil.resolveToken(httpServletRequest, REFRESH_KEY);
		System.out.println("Refresh_Token : " + refresh_token);
		if(!jwtUtil.refreshTokenValid(refresh_token)) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}

		String userId = jwtUtil.getUserInfoFromToken(refresh_token);

		String accessToken = jwtUtil.createToken(userId, "Access");
//		TokenDto tokenDto = new TokenDto(allToken.getAccessToken(), allToken.getRefreshToken());

//		RefreshTokenRedisTemplate.opsForValue().set(
//				userId,
//				tokenDto.getRefreshToken(),
//				14,
//				TimeUnit.DAYS);

//		setHeader(response, tokenDto);

		Message message = Message.setSuccess(StatusEnum.OK, "액세스 토큰 재발급 성공", accessToken);

		return new ResponseEntity<>(message, HttpStatus.OK);

	}
}
