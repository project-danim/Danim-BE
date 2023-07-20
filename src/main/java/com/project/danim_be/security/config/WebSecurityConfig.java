package com.project.danim_be.security.config;

import com.project.danim_be.security.jwt.JwtAuthenticationFilter;
import com.project.danim_be.security.jwt.JwtUtil;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

	private final JwtAuthenticationFilter jwtUtil;

	private static final String[] PERMIT_URL_ARRAY={

			"/api/user/kakao/**",
			"/api/user/google/**",
			"/api/user/naver/**",
			"/api/user/signup",
			"/api/user/login",
			"/api/user/checkId",
			"/api/user/checkNickname",
			"/api/user/delete",
			"/api/user/{ownerId}/info",
			"/api/user/{ownerId}/posts",
			"/api/user/{ownerId}/review",
			"/api/user/randomNickname",
			"/api/user/userInfo",

			"/v3/api-docs/**",
			"/swagger-ui/**",
			"/api/posts/**",
			"/ws-stomp/**",
			"/api/post/{postId}",
			"/api/post/image",
			"/api/chat/allChatRoom",
			"/api/chat/test",
			"/api/user/mailCheck",
			"/stomp",
			"/info",
			"/api/user/refreshToken"

	};

	//정적자원은 인증인가를 하지않겠다.
	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {

		return (web) -> web.ignoring()
				.requestMatchers(PathRequest.toStaticResources().atCommonLocations());

	}

	//비밀번호 암호화
	@Bean
	public PasswordEncoder passwordEncoder(){

		return new BCryptPasswordEncoder();

	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http.csrf(csrf -> csrf.disable());

		http.authorizeHttpRequests(request -> request
				.dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()
				.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
				.requestMatchers(HttpMethod.GET, "/api/post/{postId}", "/api/post/{postId}/review").permitAll()
				.requestMatchers(PERMIT_URL_ARRAY).permitAll()
				.requestMatchers("/status", "/images/**").permitAll()
				.requestMatchers("/ws/**").permitAll()
				.requestMatchers("/api/user/refreshKey").permitAll()
				.requestMatchers("/stomp").permitAll()
				.requestMatchers("/ws-stomp").permitAll()
				.requestMatchers("/health-check").permitAll()
				.anyRequest()
				.authenticated()

		);

		http
			.cors(withDefaults());

		http.addFilterBefore(jwtUtil, UsernamePasswordAuthenticationFilter.class);

		return http.build();

	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

		CorsConfiguration configuration = new CorsConfiguration();

		//접근할수있는 포트설정
//		configuration.setAllowedOrigins(Arrays.asList(
//				"http://localhost:3000",
//				"http://localhost:8080",
//				"http://127.0.0.1:3000",
//				"http://localhost:63342",
//				"http://jxy.me/**",
//				"http://jxy.me/",
//				"https://danim-https-1018737567.ap-northeast-2.elb.amazonaws.com/",
//				"http://project-danim.s3-website.ap-northeast-2.amazonaws.com/",
//				"https://da-nim.com",
//				"https://www.da-nim.com"
//		));
		configuration.addAllowedOrigin("http://localhost:3000");
		configuration.addAllowedOrigin("http://localhost:8080");
		configuration.addAllowedOrigin("http://127.0.0.1:3000");
		configuration.addAllowedOrigin("http://localhost:63342");
		configuration.addAllowedOrigin("http://jxy.me/**");
		configuration.addAllowedOrigin("http://jxy.me/");
		configuration.addAllowedOrigin("https://danim-https-1018737567.ap-northeast-2.elb.amazonaws.com/");
		configuration.addAllowedOrigin("http://project-danim.s3-website.ap-northeast-2.amazonaws.com/");
		configuration.addAllowedOrigin("https://da-nim.com");
		configuration.addAllowedOrigin("https://www.da-nim.com");

		configuration.addExposedHeader(JwtUtil.ACCESS_KEY);
		configuration.addExposedHeader(JwtUtil.REFRESH_KEY);
		configuration.addExposedHeader("Set-Cookie");
		configuration.addAllowedHeader(CorsConfiguration.ALL);

		//어떤데이터
//		configuration.setAllowedHeaders(Arrays.asList("*"));

		//모든 방식(GET, POST, PUT, DELETE 등)으로 데이터를 요청할 수 있게함
		configuration.addAllowedMethod("*");

//		configuration.setAllowedMethods(Arrays.asList("*"));

		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		//이 부분은 위에서 설정한 CORS 설정을 모든 경로에 적용
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}

}


