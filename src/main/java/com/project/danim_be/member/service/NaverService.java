package com.project.danim_be.member.service;

import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonParser;
import com.project.danim_be.common.exception.CustomException;
import com.project.danim_be.common.exception.ErrorCode;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.common.util.RandomNickname;
import com.project.danim_be.common.util.StatusEnum;
import com.project.danim_be.member.dto.LoginResponseDto;
import com.project.danim_be.member.dto.MemberRequestDto;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NaverService {

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String clientSecret;
    @Value("${spring.security.oauth2.client.provider.naver.token-uri}")
    private String tokenUrl;
    @Value("${spring.security.oauth2.client.provider.naver.user-info-uri}")
    private String userInfoUrl;

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RandomNickname randomNickname;
    private final NotificationService notificationService;

    @Transactional
    public ResponseEntity<Message> naverLogin(String code, HttpServletResponse response) throws IOException {

        JsonElement tokenData = getTokens(code);
        String accessToken = tokenData.getAsJsonObject().get("access_token").getAsString();
        System.out.println("accessToken"+accessToken);

        MemberRequestDto memberRequestDto = getNaverUserInfo(accessToken);

        RefreshToken refreshToken = new RefreshToken(tokenData.getAsJsonObject().get("refresh_token").getAsString(), memberRequestDto.getUserId(), "NAVER");
        refreshTokenRepository.save(refreshToken);
        System.out.println("refreshToken"+refreshToken);

        Member member = saveMember(memberRequestDto);

        SseEmitter sseEmitter = notificationService.connectNotification(member.getId());
        LoginResponseDto loginResponseDto = new LoginResponseDto(member, sseEmitter);

        forceLogin(member);

        createToken(member, response);

        return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "로그인 성공", loginResponseDto));
    }

    private JsonElement getTokens(String code) throws IOException {

        URL url = new URL(tokenUrl);
        // url로 연결
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        // OutputStream으로 POST 데이터를 넘겨주겠다는 옵션
        connection.setDoOutput(true);

        // params 전송
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
        String params = "grant_type=authorization_code" +
                "&client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&redirect_uri= http://localhost:8080/api/user/naver/callback" +
                "&code=" + code;
        bw.write(params);
        bw.flush();
        bw.close();

        // 응답 데이터 줄별로 읽어오기
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String oneLine;
        StringBuilder result = new StringBuilder();

        while ((oneLine = br.readLine()) != null) {
            result.append(oneLine);
        }
        br.close();

        // JSON 파싱해서 access_token만 리턴
        return JsonParser.parseString(result.toString());

//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
//
//        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
//        params.add("grant_type", "authorization_code");
//        params.add("client_id", clientId);
//        params.add("client_secret", clientSecret);
//        params.add("code", code);
//        params.add("state", state);
//
//        HttpEntity<MultiValueMap<String, String>> naverTokenRequest = new HttpEntity<>(params, headers);
//        RestTemplate rt = new RestTemplate();
//        ResponseEntity<String> naverResponse = rt.exchange(
//                tokenUrl,
//                HttpMethod.POST,
//                naverTokenRequest,
//                String.class
//        );
//
//        String responseBody = naverResponse.getBody();
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode jsonNode = objectMapper.readTree(responseBody);
//        return jsonNode.get("access_token").asText();
    }

    // 네이버에 요청해서 회원정보 받는 메소드
    public MemberRequestDto getNaverUserInfo(String accessToken) throws IOException {

        URL url = new URL(userInfoUrl);
        // url로 연결
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        // OutputStream으로 POST 데이터를 넘겨주겠다는 옵션
        connection.setDoOutput(true);

        connection.setRequestProperty("Authorization", "Bearer " + accessToken);

        // 응답 데이터 줄별로 읽어오기
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String oneLine;
        StringBuilder result = new StringBuilder();

        while ((oneLine = br.readLine()) != null) {
            result.append(oneLine);
        }
        br.close();

        // JSON 파싱해서 access_token만 리턴
        JsonElement userInfoData = JsonParser.parseString(result.toString());
        String email = String.valueOf(userInfoData.getAsJsonObject().get("response").getAsJsonObject().get("email"));
        email = email.substring(1, email.length()-1);
        return new MemberRequestDto(email);
    }

    private Member saveMember(MemberRequestDto memberRequestDto) {

        Member naverMember = memberRepository.findByUserId(memberRequestDto.getUserId()).orElse(null);
        String nickname = randomNickname.getRandomNickname();
        if(naverMember == null) {
            Member member = Member.builder()
                    .userId(memberRequestDto.getUserId())
                    .nickname(nickname)
                    .provider("NAVER")
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .isDeleted(false)
                    .build();

            System.out.println(memberRequestDto.getUserId());
            System.out.println(passwordEncoder.encode(UUID.randomUUID().toString()));
            memberRepository.save(member);
            return member;
        }
        return naverMember;
    }

    private void forceLogin(Member member) {
        UserDetails userDetails = new UserDetailsImpl(member, member.getUserId());
        if (member.getIsDeleted().equals(true)) {
            throw new CustomException(ErrorCode.DELETED_USER);
        }
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void createToken(Member member, HttpServletResponse response) {
        TokenDto tokenDto = jwtUtil.createAllToken(member.getUserId());
        System.out.println(tokenDto.getAccessToken());

        RefreshToken newToken = new RefreshToken(tokenDto.getRefreshToken(), member.getUserId(), "DANIM");
        refreshTokenRepository.save(newToken);

        setHeader(response, tokenDto);
    }

    // 네이버 연결 해제
    public void naverSignout(Member member) throws IOException {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUserIdAndProvider(member.getUserId(), "NAVER");

        JsonElement newToken = newTokenOrDelete(refreshToken.get().getRefreshToken(), "newToken");
        String accessToken = newToken.getAsJsonObject().get("access_token").getAsString();

        JsonElement delete = newTokenOrDelete(accessToken, "delete");
        String result = delete.getAsJsonObject().get("result").getAsString();

        if(result.equals("success")) {
        } else {
            throw new CustomException(ErrorCode.FAIL_SIGNOUT);
        }

        refreshTokenRepository.delete(refreshToken.get());
        refreshTokenRepository.delete(refreshTokenRepository.findByUserIdAndProvider(member.getUserId(), "DANIM").get());
    }

    public JsonElement newTokenOrDelete(String token, String type) throws IOException {
        URL url = new URL(tokenUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        // params 전송
        if(type.equals("newToken")) {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            String params = "grant_type=refresh_token" +
                    "&client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&refresh_token=" + token;
            bw.write(params);
            bw.flush();
            bw.close();
        } else if(type.equals("delete")) {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            String params = "grant_type=delete" +
                    "&client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&access_token=" + token +
                    "&service_provider=NAVER";
            bw.write(params);
            bw.flush();
            bw.close();
        }

        // 응답 데이터 줄별로 읽어오기
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String oneLine;
        StringBuilder result = new StringBuilder();

        while ((oneLine = br.readLine()) != null) {
            result.append(oneLine);
        }
        br.close();
        return JsonParser.parseString(result.toString());
    }

    
    private void setHeader(HttpServletResponse response, TokenDto tokenDto) {
        response.addHeader(JwtUtil.ACCESS_KEY, tokenDto.getAccessToken());
        response.addHeader(JwtUtil.REFRESH_KEY, tokenDto.getRefreshToken());
    }
}
