package com.project.danim_be.member.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.JsonElement;
import com.nimbusds.jose.shaded.gson.JsonParser;
import com.project.danim_be.common.exception.CustomException;
import com.project.danim_be.common.exception.ErrorCode;
import com.project.danim_be.common.util.Message;
import com.project.danim_be.common.util.RandomNickname;
import com.project.danim_be.common.util.StatusEnum;
import com.project.danim_be.member.dto.KakaoMemberInfoDto;
import com.project.danim_be.member.entity.Member;
import com.project.danim_be.member.repository.MemberRepository;
import com.project.danim_be.security.auth.UserDetailsImpl;
import com.project.danim_be.security.jwt.JwtUtil;
import com.project.danim_be.security.jwt.TokenDto;
import com.project.danim_be.security.refreshToken.RefreshToken;
import com.project.danim_be.security.refreshToken.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {

	private final JwtUtil jwtUtil;
	private final RefreshTokenRepository refreshTokenRepository;

	private final MemberRepository memberRepository;
	@Value("${kakao.client.id}")
	private String clientId;
	@Value("${kakao.redirect.uri}")
	private String redirectUri;
	@Value("${kakao.token.url}")
	private String tokenUrl;
	@Value("${kakao.info.url}")
	private String userInfoUrl;
	@Value("${kakao.unlink.url}")
	private String unlinkUrl;


	public ResponseEntity<Message> kakaoLogin(String code, HttpServletResponse response) throws JsonProcessingException {
		JsonNode tokenData = getToken(code);
		String accessToken = tokenData.get("access_token").asText();
		System.out.println("accessToken : "+accessToken);

		KakaoMemberInfoDto kakaoUserInfo = getKakaoMemberInfo(accessToken);
		Member kakaoUser = kakaoSignup(kakaoUserInfo);

		String refreshTokenValue = tokenData.get("refresh_token").asText();
		System.out.println("refreshToken : "+refreshTokenValue);
		RefreshToken refreshToken = new RefreshToken(refreshTokenValue, kakaoUserInfo.getEmail(), "KAKAO");
		refreshTokenRepository.save(refreshToken);

		forceLogin(kakaoUser);
		System.out.println(kakaoUser.getUserId());

		TokenDto tokenDto = jwtUtil.createAllToken(kakaoUser.getUserId());
		setHeader(response, tokenDto);


		RefreshToken newToken = new RefreshToken(tokenDto.getRefreshToken(), kakaoUser.getUserId(), "DANIM");
		refreshTokenRepository.save(newToken);

		System.out.println(tokenDto.getAccessToken());

		Message message = Message.setSuccess(StatusEnum.OK,"로그인 성공", kakaoUser.getId());
		return new ResponseEntity<>(message, HttpStatus.OK);
		// return accessToken;
	}

	private JsonNode getToken(String code) throws JsonProcessingException {
		// HTTP Header 생성
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

		// HTTP Body 생성
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "authorization_code");
		body.add("client_id", clientId);
		body.add("redirect_uri", redirectUri);
		body.add("code", code);

		// HTTP 요청 보내기
		HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest =
			new HttpEntity<>(body, headers);
		RestTemplate rt = new RestTemplate();
		ResponseEntity<String> response = rt.exchange(
			tokenUrl,
			HttpMethod.POST,
			kakaoTokenRequest,
			String.class
		);

		// HTTP 응답 (JSON) -> 액세스 토큰 파싱
		String responseBody = response.getBody();
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(responseBody);


		return jsonNode;
	}

	private KakaoMemberInfoDto getKakaoMemberInfo(String accessToken) throws JsonProcessingException {
		// HTTP Header 생성
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer " + accessToken);
		headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

		// HTTP 요청 보내기
		HttpEntity<MultiValueMap<String, String>> MemberInfoRequest = new HttpEntity<>(headers);
		RestTemplate rt = new RestTemplate();
		ResponseEntity<String> response = rt.exchange(
			userInfoUrl,
			HttpMethod.POST,
			MemberInfoRequest,
			String.class
		);

		String responseBody = response.getBody();
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(responseBody);
		Long id = jsonNode.get("id").asLong();//아이디
		String email = jsonNode.get("kakao_account").get("email").asText();	// 이메일
		String gender = jsonNode.get("kakao_account").get("gender").asText();//성별
		String age_range = jsonNode.get("kakao_account").get("age_range").asText(); // 연령대
		System.out.println("id = "+id);
		System.out.println("email = "+email);
		System.out.println("gender = "+gender);
		System.out.println("ageRange = "+ age_range);

		log.info("카카오 사용자 정보: " + id +"// email"+email);
		return new KakaoMemberInfoDto(email,gender,age_range);
	}

	public Member kakaoSignup(KakaoMemberInfoDto kakaoMemberInfoDto) {
		// DB 에 중복된 Kakao Id 가 있는지 확인
		String email = kakaoMemberInfoDto.getEmail();
		String nickname = RandomNickname.getRandomNickname();
		Member kakaoMember = memberRepository.findByUserId(email)
			.orElse(null);
		if (kakaoMember == null) {

			String password = UUID.randomUUID().toString();
			// kakaoMember = new Member(email, password, ageRange,gender,nickname);
			Member member = Member.builder()
				.userId(kakaoMemberInfoDto.getEmail())
				.ageRange(kakaoMemberInfoDto.getAgeRange())
				.gender(kakaoMemberInfoDto.getGender())
				.nickname(nickname)
				.password(password)
				.provider("KAKAO")
				.isDeleted(false)
				.build();

			System.out.println(nickname);

			memberRepository.save(member);
			return member;
		}
		return kakaoMember;
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
	public void kakaoSignout(Member member) throws IOException {

		Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUserIdAndProvider(member.getUserId(), "KAKAO");
		System.out.println("여기");
		JsonElement newToken = newTokenOrDelete(refreshToken.get().getRefreshToken(), "newToken");
		String accessToken = newToken.getAsJsonObject().get("access_token").getAsString();

		JsonElement delete = newTokenOrDelete(accessToken, "delete");
		long userId = delete.getAsJsonObject().get("id").getAsLong();

		if(userId != 0) {
			// Success, you can add more actions here if necessary
		} else {
			throw new CustomException(ErrorCode.FAIL_SIGNOUT);
		}

		refreshTokenRepository.delete(refreshToken.get());
		refreshTokenRepository.delete(refreshTokenRepository.findByUserIdAndProvider(member.getUserId(), "DANIM").get());
	}

	public JsonElement newTokenOrDelete(String token, String type) throws IOException {
		URL url;
		HttpURLConnection connection = null;

		if(type.equals("newToken")) {
			url = new URL(tokenUrl);

			connection = (HttpURLConnection) url.openConnection();

			connection.setRequestMethod("POST");
			connection.setDoOutput(true);

			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
			String params = "grant_type=refresh_token" +
				"&client_id=" + clientId +
				"&refresh_token=" + token;
			bw.write(params);
			bw.flush();
			bw.close();
		} else if(type.equals("delete")) {
			url = new URL(unlinkUrl);
			connection = (HttpURLConnection) url.openConnection();

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Authorization", "Bearer " + token);
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
