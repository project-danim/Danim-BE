package com.project.danim_be.member.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.member.dto.CheckIdRequestDto;
import com.project.danim_be.member.dto.CheckNicknameRequestDto;
import com.project.danim_be.member.dto.LoginRequestDto;
import com.project.danim_be.member.dto.SignupRequestDto;
import com.project.danim_be.member.service.GoogleService;
import com.project.danim_be.member.service.KakaoService;
import com.project.danim_be.member.service.MemberService;
import com.project.danim_be.member.service.NaverService;
import com.project.danim_be.security.auth.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;
	private final NaverService naverService;
	private final KakaoService kakaoService;
	private final GoogleService googleService;

	//일반 회원가입
	@PostMapping("/signup")
	public ResponseEntity<Message> signup(@Valid @RequestBody SignupRequestDto signupRequestDto, BindingResult bindingResult){
		if(bindingResult.hasErrors()){
			String errMessage =  bindingResult.getAllErrors().get(0).getDefaultMessage();
			Message apiResult = Message.builder()
					.statusCode(HttpStatus.BAD_REQUEST.value())
					.message(errMessage)
					.build();
			return ResponseEntity.badRequest().body(apiResult);
		}else{
			return memberService.signup(signupRequestDto);
		}
	}

	//일반 회원가입 아이디 중복 검사
	@PostMapping("/checkId")
	public ResponseEntity<Message> checkId(@RequestBody CheckIdRequestDto checkIdRequestDto) {
		
		return memberService.checkId(checkIdRequestDto);
	}

	//일반 회원가입 닉네임 중복 검사
	@PostMapping("/checkNickname")
	public ResponseEntity<Message> checkNickname(@RequestBody CheckNicknameRequestDto checkNicknameRequestDto) {
		return memberService.checkNickname(checkNicknameRequestDto);
  }

	//회원가입시 랜덤 닉네임 생성
	@GetMapping("/randomNickname")
	public ResponseEntity<Message> nicknameCreate() {
		return memberService.nicknameCreate();
	}

	//로그인
	@PostMapping("/login")
	public ResponseEntity<Message> login(@RequestBody LoginRequestDto requestDto, HttpServletResponse response ){
		return memberService.login(requestDto,response);
	}

	//로그아웃
	@DeleteMapping("/logout")
	public ResponseEntity<Message> logout(@AuthenticationPrincipal UserDetailsImpl userDetails, HttpServletRequest request){
		return memberService.logout(userDetails.getMember(), request);
	}

	//네이버 소셜 로그인
	@GetMapping("/naver/callback")
	public ResponseEntity<Message> naverLogin(@RequestParam String code, HttpServletResponse response) throws IOException {
		return naverService.naverLogin(code, response);
	}

	//카카오 소셜 로그인
	@GetMapping("/kakao/callback")
	public ResponseEntity<Message> kakaoLogin(@RequestParam String code,HttpServletResponse response) throws JsonProcessingException {
		System.out.println(code);
		return kakaoService.kakaoLogin(code,response);
	}

	//구글 소셜 로그인
	@GetMapping("/google/callback")
	public ResponseEntity<Message> googleLogin(@RequestParam String code, HttpServletResponse response) {
		// registrationId 이 부분은 소셜 로그인을 공통으로 구현할때 쓰임 .registrationId 값에 @RequestParam으로 kakao나 naver, google이 들어가면
		// 그에 맞는 프로퍼티스에서 값을 가져와 같은 메서드로 서로 다른 소셜 로그인 구현 가능
		System.out.println(code);
		return googleService.socialLogin(code, response);
	}

	//회원 탈퇴
	@DeleteMapping("/delete")
	public ResponseEntity<Message> signOut(@AuthenticationPrincipal UserDetailsImpl userDetails) {
		System.out.println("1. "+userDetails.getMember().getUserId());
		return memberService.signout(userDetails.getMember());
	}
}

