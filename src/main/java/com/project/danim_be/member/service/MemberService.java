package com.project.danim_be.member.service;

import com.project.danim_be.common.exception.CustomException;
import com.project.danim_be.common.exception.ErrorCode;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.common.util.RandomNickname;
import com.project.danim_be.common.util.StatusEnum;
import com.project.danim_be.member.dto.*;
import com.project.danim_be.member.entity.Member;
import com.project.danim_be.member.repository.MemberRepository;
import com.project.danim_be.security.jwt.JwtUtil;
import com.project.danim_be.security.jwt.TokenDto;
import com.project.danim_be.security.refreshToken.RefreshToken;
import com.project.danim_be.security.refreshToken.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Optional;

import static com.project.danim_be.common.exception.ErrorCode.FAIL_SIGNOUT;
import static com.project.danim_be.common.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtUtil jwtUtil;
	private final NaverService naverService;
	private final KakaoService kakaoService;
	private final GoogleService googleService;

	//회원가입
	@Transactional
	public ResponseEntity<Message> signup(SignupRequestDto signupRequestDto) {

		String userId = signupRequestDto.getUserId();
		String password = passwordEncoder.encode(signupRequestDto.getPassword());
		String nickname = signupRequestDto.getNickname();
		String ageRange = signupRequestDto.getAgeRange();

		Member member = Member.builder()
				.userId(userId)
				.ageRange(ageRange)
				.gender(null)
				.nickname(nickname)
				.password(password)
				.provider("GENERAL")
				.isDeleted(false)
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
	public ResponseEntity<Message> nicknameCreate() {
		String nickname = RandomNickname.getRandomNickname();
		while (memberRepository.findByNickname(nickname).isPresent()){
			nickname = RandomNickname.getRandomNickname();
		}
		return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "랜덤 닉네임 생성완료", nickname));
	}

	//로그인
	@Transactional
	public ResponseEntity<Message> login(LoginRequestDto requestDto, HttpServletResponse response) {

		String userId = requestDto.getUserId();
		String password = requestDto.getPassword();

		Member member = memberRepository.findByUserId(userId).orElseThrow(
				() -> new CustomException(ErrorCode.ID_NOT_FOUND)
		);

		if (!passwordEncoder.matches(password, member.getPassword())) {
			throw new CustomException(ErrorCode.INVALID_PASSWORD);
		}
		TokenDto tokenDto = jwtUtil.createAllToken(userId);

		Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUserId(member.getUserId());
		if (refreshToken.isPresent()) {
			refreshTokenRepository.save(refreshToken.get().updateToken(tokenDto.getRefreshToken()));
		} else {

			RefreshToken newToken = new RefreshToken(tokenDto.getRefreshToken(), member.getUserId(), "DANIM");

			refreshTokenRepository.save(newToken);
		}
		setHeader(response, tokenDto);

		LoginResponseDto loginResponseDto =new LoginResponseDto(member);
		Message message = Message.setSuccess(StatusEnum.OK, "로그인 성공", loginResponseDto);
		return new ResponseEntity<>(message, HttpStatus.OK);
	}

	//로그아웃
	@Transactional
	public ResponseEntity<Message> logout(Member member, HttpServletRequest request) {
		Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUserId(member.getUserId());

		String accessToken = request.getHeader("ACCESS_KEY").substring(7);
		if(refreshToken.isPresent()){
			Long tokenTime = jwtUtil.getExpirationTime(accessToken);
			refreshTokenRepository.deleteByUserId(member.getUserId());
			Message message = Message.setSuccess(StatusEnum.OK,"로그아웃 성공", member.getUserId());
			return new ResponseEntity<>(message, HttpStatus.OK);
		}
		throw new CustomException(USER_NOT_FOUND);
	}

	// 회원 탈퇴
	@Transactional
	public ResponseEntity<Message> signout(Member member) {
		System.out.println("탈퇴 접근?");
		member = memberRepository.findById(member.getId()).orElseThrow(() -> new CustomException(USER_NOT_FOUND));

		switch (member.getProvider()) {
			case "NAVER" -> {
				try {
					naverService.naverSignout(member);
				} catch (IOException e) {
					throw new CustomException(FAIL_SIGNOUT);
				}
			}
			case "KAKAO" -> {
				try {
					kakaoService.kakaoSignout(member);
				} catch (IOException e) {
					throw new CustomException(FAIL_SIGNOUT);
				}
			}
//			case "GOOGLE" -> {
//				try {
//					kakaoService.googleSignout(member);
//				} catch (IOException e) {
//					throw new CustomException(FAIL_SIGNOUT);
//				}
//			}
		}
		member.signOut();
		return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "탈퇴 성공"));
	}

	// 헤더 셋팅
	private void setHeader(HttpServletResponse response, TokenDto tokenDto) {
		response.addHeader(JwtUtil.ACCESS_KEY, tokenDto.getAccessToken());
		response.addHeader(JwtUtil.REFRESH_KEY, tokenDto.getRefreshToken());
	}


}
