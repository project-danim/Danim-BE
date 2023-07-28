package com.project.danim_be.security.jwt;

import com.project.danim_be.security.auth.UserDetailsServiceImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

	private static final String BEARER_PREFIX = "Bearer ";
	public static final String ACCESS_KEY = "ACCESS_KEY";
	public static final String REFRESH_KEY = "REFRESH_KEY";
	private static final long ACCESS_TIME = Duration.ofMinutes(60).toMillis();
	private static final long REFRESH_TIME = Duration.ofDays(14).toMillis();

	@Value("${jwt.secret.key}")
	private String secretKey;
	private Key key;
	private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
	private final UserDetailsServiceImpl userDetailsService;
	private final RedisTemplate<String, String> RefreshTokenRedisTemplate;

	@PostConstruct
	public void init() {

		byte[] bytes = Base64.getDecoder().decode(secretKey);
		key = Keys.hmacShaKeyFor(bytes);

	}

	public TokenDto createAllToken(String userId) {

		return new TokenDto(createToken(userId, "Access"), createToken(userId, REFRESH_KEY));

	}

	public String createToken(String userId, String token) {

		Date date = new Date();
		long tokenType = token.equals("Access") ? ACCESS_TIME : REFRESH_TIME;

		return
				BEARER_PREFIX +
			Jwts.builder()
				.setSubject(userId)
				.setExpiration(new Date(date.getTime() + tokenType))
				.setIssuedAt(date)
				.signWith(key, signatureAlgorithm)
				.compact();

	}

	// 토큰 검증
	public boolean validateToken(String token) {

		try {
			Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
			return true;
		} catch (SecurityException | MalformedJwtException e) {
			log.info("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.");
		} catch (ExpiredJwtException e) {
			log.info("Expired JWT token, 만료된 JWT token 입니다.");
		} catch (UnsupportedJwtException e) {
			log.info("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
		} catch (IllegalArgumentException e) {
			log.info("JWT claims is empty, 잘못된 JWT 토큰 입니다.");
		}
		return false;

	}

	// 토큰에서 사용자 정보 가져오기
	public String getUserInfoFromToken(String token) {

		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getSubject();

	}

	// header 토큰을 가져오기
	public String resolveToken(HttpServletRequest request, String tokenName) {

		String token = null;

		if (tokenName.equals("REFRESH_KEY")){
//			String tokenName = token.equals("REFRESH_KEY") ? ACCESS_KEY : REFRESH_KEY;
			Cookie[] cookies = request.getCookies();
			System.out.println("Cookie : " + Arrays.toString(cookies));
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals("REFRESH_KEY")) {
						token = cookie.getValue();
					}
				}
			}
		} else {
			String bearerToken = request.getHeader("ACCESS_KEY");
			if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
				token = bearerToken
					.substring(7);
			}
		}
		return token;
	}

	// 인증 객체 생성
	public Authentication createAuthentication(String username) {

		UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

	}

	public boolean refreshTokenValid(String token) {
		if (!validateToken(token)) return false;
		String refreshToken = RefreshTokenRedisTemplate.opsForValue().get(getUserInfoFromToken(token));
		return token.equals(refreshToken);
//				.substring(7));

	}

	public void setCookieAccessToken(HttpServletResponse response, String accessToken) {

//		Cookie accessTokenCookie = new Cookie("ACCESS_KEY", accessToken);
//		accessTokenCookie.setHttpOnly(true);
//		accessTokenCookie.setSecure(true);
//		response.addCookie(accessTokenCookie);
		response.setHeader(ACCESS_KEY, accessToken);
//		return ResponseEntity.ok(Message.setSuccess(StatusEnum.OK, "AccessToken 재발급", accessToken));
	}

	public long getExpirationTime(String token) {

		// 토큰에서 만료 시간 정보를 추출
		Claims claims = Jwts.parser()
			.setSigningKey(secretKey)
			.parseClaimsJws(token)
			.getBody();

		// 현재 시간과 만료 시간의 차이를 계산하여 반환
		Date expirationDate = claims.getExpiration();
		Date now = new Date();
		return (expirationDate.getTime() - now.getTime()) / 1000;

	}

}
