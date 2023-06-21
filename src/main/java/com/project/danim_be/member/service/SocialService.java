package com.project.danim_be.member.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.danim_be.common.exception.CustomException;
import com.project.danim_be.common.exception.ErrorCode;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.common.util.RandomNickname;
import com.project.danim_be.common.util.StatusEnum;
import com.project.danim_be.member.dto.RequestDto.MemberRequestDto;
import com.project.danim_be.member.dto.ResponseDto.LoginResponseDto;
import com.project.danim_be.member.entity.Member;
import com.project.danim_be.member.repository.MemberRepository;
import com.project.danim_be.notification.service.NotificationService;
import com.project.danim_be.security.auth.UserDetailsImpl;
import com.project.danim_be.security.jwt.JwtUtil;
import com.project.danim_be.security.jwt.TokenDto;
import com.project.danim_be.security.refreshToken.RefreshToken;
import com.project.danim_be.security.refreshToken.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialService {

    private final Environment env;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RandomNickname randomNickname;
    private final NotificationService notificationService;

    @Transactional
    public ResponseEntity<Message> socialLogin(String provider, String code, HttpServletResponse response) throws JsonProcessingException {

        JsonNode tokenData = getTokens(code, provider);
        String accessToken = tokenData.get("access_token").asText();
        System.out.println("accessToken : " + accessToken);

        MemberRequestDto memberRequestDto = getUserInfo(accessToken, provider);

        // google은 refreshToken을 안줌
        if(!provider.equals("GOOGLE")) {
            List<RefreshToken> refreshTokenList = refreshTokenRepository.findAllByUserId(memberRequestDto.getUserId());
            if(refreshTokenList.isEmpty()) {
                RefreshToken newRefreshToken = new RefreshToken(tokenData.get("refresh_token").asText(), memberRequestDto.getUserId(), provider);
                refreshTokenRepository.save(newRefreshToken);
            } else {
                for(RefreshToken refreshToken : refreshTokenList) {
                    if(refreshToken.getProvider().equals(provider)) {
                        refreshTokenRepository.save(refreshToken.updateToken(tokenData.get("refresh_token").asText()));
                    }
                }
            }
        }

        Member member = saveMember(memberRequestDto, provider);

        forceLogin(member);
        System.out.println("userId : " + member.getUserId());

        createToken(member, response);

        Boolean isExistMember = memberRepository.existsByUserId(member.getUserId());

        SseEmitter sseEmitter = notificationService.connectNotification(member.getId());
        LoginResponseDto loginResponseDto = new LoginResponseDto(member, sseEmitter, isExistMember);

        return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "로그인 성공", loginResponseDto));
    }

    private JsonNode getTokens(String code, String provider) throws JsonProcessingException {

        String clientId = env.getProperty("spring.security.oauth2.client.registration."+provider.toLowerCase()+".client-id");
        String clientSecret = env.getProperty("spring.security.oauth2.client.registration."+provider.toLowerCase()+".client-secret");
        String redirectUri = env.getProperty("spring.security.oauth2.client.registration."+provider.toLowerCase()+".redirect-uri");
        String tokenUri = env.getProperty("spring.security.oauth2.client.provider."+provider.toLowerCase()+".token-uri");

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);

        if(!provider.equals("KAKAO")){
            body.add("client_secret", clientSecret);
        }
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> getTokenRequest = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                tokenUri,
                HttpMethod.POST,
                getTokenRequest,
                String.class
        );

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        return jsonNode;
    }

    // 회원정보 받는 메소드
    public MemberRequestDto getUserInfo(String accessToken, String provider) throws JsonProcessingException {

        String userInfoUri = env.getProperty("spring.security.oauth2.client.provider." + provider.toLowerCase() + ".user-info-uri");

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> MemberInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                userInfoUri,
                HttpMethod.GET,
                MemberInfoRequest,
                String.class
        );

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode userInfoData = objectMapper.readTree(responseBody);
        System.out.println("userInfoData : " + userInfoData);
        String email = null;

        switch (provider) {
            case "NAVER" -> {
                email = userInfoData.get("response").get("email").asText();
                break;
            }
            case "KAKAO" -> {
                email = userInfoData.get("kakao_account").get("email").asText();
                break;
            }
            case "GOOGLE" -> {
                email = userInfoData.get("email").asText();
                break;
            }
        }
        return new MemberRequestDto(email);
    }

    private Member saveMember(MemberRequestDto memberRequestDto, String provider) {

        Member socialMember = memberRepository.findByUserId(memberRequestDto.getUserId()).orElse(null);

        if(socialMember == null) {
            String password = passwordEncoder.encode(UUID.randomUUID().toString());
            String nickname = randomNickname.getRandomNickname();
            Member member = Member.builder()
                    .userId(memberRequestDto.getUserId())
                    .nickname(nickname)
                    .provider(provider)
                    .password(password)
                    .isDeleted(false)
                    .score(20.0)
                    .imageUrl("https://danimdata.s3.ap-northeast-2.amazonaws.com/avatar.png")
                    .build();

            memberRepository.save(member);
            return member;
        }
        return socialMember;
    }

    private void forceLogin(Member member) {

        UserDetails userDetails = new UserDetailsImpl(member, member.getUserId());

        if (member.getIsDeleted().equals(true)) {
            throw new CustomException(ErrorCode.DELETED_USER);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void createToken(Member member, HttpServletResponse response) {

        String userId = member.getUserId();
        TokenDto tokenDto = jwtUtil.createAllToken(userId);
        int count = 0;

        List<RefreshToken> refreshTokenList = refreshTokenRepository.findAllByUserId(userId);
        for(RefreshToken refreshToken : refreshTokenList) {
            if(refreshToken.getProvider().equals("DANIM")){
                count++;
            }
        }

        for(RefreshToken refreshToken : refreshTokenList) {
            if(!refreshToken.getProvider().equals("DANIM") && count == 0) {
                RefreshToken newToken = new RefreshToken(tokenDto.getRefreshToken(), userId, "DANIM");
                refreshTokenRepository.save(newToken);
            } else if(refreshToken.getProvider().equals("DANIM")) {
                refreshTokenRepository.save(refreshToken.updateToken(tokenDto.getRefreshToken()));
            }
        }

        setHeader(response, tokenDto);
    }

    // 연결 해제
    public void naverSignout(Member member) throws IOException {

        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUserIdAndProvider(member.getUserId(), member.getProvider());

        JsonNode newToken = newTokenOrDelete(refreshToken.get().getRefreshToken(), "newToken", member.getProvider());
        String accessToken = newToken.get("access_token").asText();

        JsonNode delete = newTokenOrDelete(accessToken, "delete", member.getProvider());

        if(member.getProvider().equals("KAKAO")) {
            long userId = delete.get("id").asLong();

            if(userId != 0) {
            } else {
                throw new CustomException(ErrorCode.FAIL_SIGNOUT);
            }
        }

        if(member.getProvider().equals("NAVER")) {
            String result = delete.get("result").asText();

            if(result.equals("success")) {
            } else {
                throw new CustomException(ErrorCode.FAIL_SIGNOUT);
            }
        }

        refreshTokenRepository.delete(refreshToken.get());
        refreshTokenRepository.delete(refreshTokenRepository.findByUserIdAndProvider(member.getUserId(), "DANIM").get());
    }

    public JsonNode newTokenOrDelete(String token, String type, String provider) throws IOException {

        String clientId = env.getProperty("spring.security.oauth2.client.registration." + provider.toLowerCase() + ".client-id");
        String clientSecret = env.getProperty("spring.security.oauth2.client.registration." + provider.toLowerCase() + ".client-secret");
        String tokenUri = env.getProperty("spring.security.oauth2.client.provider." + provider.toLowerCase() + ".token-uri");

        ResponseEntity<String> response = null;

        // params 전송
        if (type.equals("newToken")) {

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", token);

            HttpEntity<MultiValueMap<String, String>> newTokenRequest =
                    new HttpEntity<>(body, headers);
            RestTemplate rt = new RestTemplate();
            response = rt.exchange(
                    tokenUri,
                    HttpMethod.POST,
                    newTokenRequest,
                    String.class
            );

            // HTTP 응답 (JSON) -> 액세스 토큰 파싱
            String responseBody = response.getBody();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            return jsonNode;
        } else {
            switch (provider) {
                case "NAVER": {

                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

                    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                    body.add("grant_type", "delete");
                    body.add("client_id", clientId);
                    body.add("client_secret", clientSecret);
                    body.add("access_token", token);
                    body.add("service_provider", "NAVER");

                    HttpEntity<MultiValueMap<String, String>> naverRequest = new HttpEntity<>(body, headers);
                    RestTemplate rt = new RestTemplate();
                    response = rt.exchange(
                            tokenUri,
                            HttpMethod.POST,
                            naverRequest,
                            String.class
                    );
                    break;
                }
                case "KAKAO": {

                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
                    headers.add("Authorization", "Bearer " + token);

                    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                    HttpEntity<MultiValueMap<String, String>> kakaoRequest = new HttpEntity<>(body, headers);
                    RestTemplate rt = new RestTemplate();
                    response = rt.exchange(
                            "https://kapi.kakao.com/v1/user/unlink",
                            HttpMethod.POST,
                            kakaoRequest,
                            String.class
                    );
                    break;
                }
            }

            // HTTP 응답 (JSON) -> 액세스 토큰 파싱
            String responseBody = response.getBody();
            System.out.println(responseBody);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode;

        }
    }

    private void setHeader(HttpServletResponse response, TokenDto tokenDto) {
        response.addHeader(JwtUtil.ACCESS_KEY, tokenDto.getAccessToken());
        response.addHeader(JwtUtil.REFRESH_KEY, tokenDto.getRefreshToken());
    }
}
