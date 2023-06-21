package com.project.danim_be.member.controller;

import com.project.danim_be.common.Anotation.LogExecutionTime;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.member.dto.RequestDto.*;
import com.project.danim_be.member.service.MailService;
import com.project.danim_be.member.service.MemberService;
import com.project.danim_be.member.service.SocialService;
import com.project.danim_be.security.auth.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Tag(name = "MemberController", description = "멤버 API")
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;
	private final SocialService socialService;
	private final MailService mailService;

	//일반 회원가입
	@Operation(summary = "일반 회원가입 API", description = "일반 회원가입")
	@PostMapping("/signup")
	@LogExecutionTime
	public ResponseEntity<Message> signup(@Valid @RequestBody SignupRequestDto signupRequestDto){
		return memberService.signup(signupRequestDto);
	}

	//일반 회원가입 아이디 중복 검사
	@Operation(summary = "일반 회원가입 아이디 중복 검사 API", description = "아이디 중복 검사")
	@PostMapping("/checkId")
	public ResponseEntity<Message> checkId(@Valid @RequestBody CheckIdRequestDto checkIdRequestDto) {
		return memberService.checkId(checkIdRequestDto);
	}

	//일반 회원가입 닉네임 중복 검사
	@Operation(summary = "일반 회원가입 닉네임 중복 검사 API", description = "닉네임 중복 검사")
	@PostMapping("/checkNickname")
	public ResponseEntity<Message> checkNickname(@Valid @RequestBody CheckNicknameRequestDto checkNicknameRequestDto) {
		return memberService.checkNickname(checkNicknameRequestDto);
  }

	//회원가입시 랜덤 닉네임 생성
	@Operation(summary = "회원가입시 랜덤 닉네임 생성 API", description = "랜덤 닉네임 생성")
	@GetMapping("/randomNickname")
	public ResponseEntity<Message> nicknameCreate() {
		return memberService.nicknameCreate();
	}

	//일반 회원가입시 이메일 인증
	@Operation(summary = "일반 회원가입 이메일 인증", description = "이메일 인증")
	@GetMapping("/mailCheck")
	@ResponseBody
	public String mailCheck(String email) {
		return mailService.mailCheck(email);
	}

	//로그인
	@Operation(summary = "일반 로그인 API", description = "일반 로그인")
	@PostMapping("/login")
	@LogExecutionTime
	public ResponseEntity<Message> login(@RequestBody LoginRequestDto requestDto, HttpServletResponse response ){
		return memberService.login(requestDto,response);
	}

	//로그아웃
	@Operation(summary = "로그아웃 API", description = "로그아웃")
	@DeleteMapping("/logout")
	@LogExecutionTime
	public ResponseEntity<Message> logout(@AuthenticationPrincipal UserDetailsImpl userDetails, HttpServletRequest request){
		return memberService.logout(userDetails.getMember(), request);
	}

	//네이버 소셜 로그인
	@Operation(summary = "네이버 소셜 로그인 API", description = "네이버 소셜 로그인")
	@GetMapping("/naver/callback")
	public ResponseEntity<Message> naverLogin(@RequestParam String code, HttpServletResponse response) throws IOException {
		return socialService.socialLogin("NAVER", code, response);
	}

	//카카오 소셜 로그인
	@Operation(summary = "카카오 소셜 로그인 API", description = "카카오 소셜 로그인")
	@GetMapping("/kakao/callback")
	@LogExecutionTime
	public ResponseEntity<Message> kakaoLogin(@RequestParam String code, HttpServletResponse response) throws IOException {
		return socialService.socialLogin("KAKAO", code, response);
	}

	//구글 소셜 로그인
	@Operation(summary = "구글 소셜 로그인 API", description = "구글 소셜 로그인")
	@GetMapping("/google/callback")
	public ResponseEntity<Message> googleLogin(@RequestParam String code, HttpServletResponse response) throws IOException {
		return socialService.socialLogin("GOOGLE", code, response);
	}

	// 소셜 로그인 시 추가 회원 정보 작성
	@Operation(summary = "추가 사용자 정보 작성 API", description = "추가 사용자 정보 작성")
	@PostMapping("/userInfo")
	public ResponseEntity<Message> addUserInfo(@Valid @RequestBody UserInfoRequestDto userInfoRequestDto) {
		return memberService.addUserInfo(userInfoRequestDto);
	}

	//회원 탈퇴
	@Operation(summary = "회원 탈퇴 API", description = "회원 탈퇴")
	@DeleteMapping("/delete")
	@LogExecutionTime
	public ResponseEntity<Message> signOut(@AuthenticationPrincipal UserDetailsImpl userDetails) {
		return memberService.signOut(userDetails.getMember());
	}
}

