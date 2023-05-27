package com.project.danim_be.member.service;

import com.project.danim_be.common.exception.CustomException;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.member.dto.CheckIdRequestDto;
import com.project.danim_be.member.dto.CheckNicknameRequestDto;
import com.project.danim_be.member.dto.SignupRequestDto;
import com.project.danim_be.member.entity.Member;
import com.project.danim_be.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Nested
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private Member member;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private SignupRequestDto signupRequestDto;
    @Mock
    private CheckIdRequestDto checkIdRequestDto;
    @Mock
    private CheckNicknameRequestDto checkNicknameRequestDto;

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signupTest() {
        // given
        signupRequestDto = new SignupRequestDto("limslki333@hanmail.net",
                "1234", "우아한고양이", "30-39");

        // when
        ResponseEntity<Message> result = memberService.signup(signupRequestDto);

        // then
        assertEquals(result.getBody().getMessage(), "회원 가입 성공");
//        assertEquals(member.getUserId(), signupRequestDto.getUserId());
//        assertEquals(member.getPassword(), signupRequestDto.getPassword());
//        assertEquals(member.getNickname(), signupRequestDto.getNickname());
//        assertEquals(member.getAgeRange(), signupRequestDto.getAgeRange());
    }

    @Test
    @DisplayName("userId 중복 예외 확인")
    void duplicateMemberTest() {
        // given
        checkIdRequestDto = new CheckIdRequestDto("limslki333@hanmail.net");
        checkIdRequestDto = new CheckIdRequestDto("limslki333@hanmail.net");

        // when, then
        try {
            memberService.checkId(checkIdRequestDto);
        } catch (CustomException e) {
            assertEquals("중복된 아이디 입니다.", e.getErrorCode().getDetail());
        }
    }

    @Test
    @DisplayName("닉네임 중복 예외 확인")
    void duplicateNicknameTest() {
        // given
        checkNicknameRequestDto = new CheckNicknameRequestDto("우아한사자");
        checkNicknameRequestDto = new CheckNicknameRequestDto("우아한사자");

        // when, then
        try {
            memberService.checkNickname(checkNicknameRequestDto);
        } catch (CustomException e) {
            assertEquals("중복된 닉네임 입니다.", e.getErrorCode().getDetail());
        }
    }

//    @Test
//    @DisplayName("로그인 정상 테스트")
//    void login() {
//
//        //given
//
//        LoginRequestDto loginRequestDto = new LoginRequestDto("user1234@naver.com", "user1234!");
//        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
//
//        //when
//        ResponseEntity<Message> response = memberService.login(loginRequestDto, mockResponse);
//
//        //then
//        assertEquals(response.getBody().getMessage(), "로그인 성공");
//
//    }

}