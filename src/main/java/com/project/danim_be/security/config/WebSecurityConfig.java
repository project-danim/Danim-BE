package com.project.danim_be.security.config;


import com.project.danim_be.security.jwt.JwtAuthenticationFilter;
import com.project.danim_be.security.jwt.JwtUtil;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
		// "/api/user/logout",
		"/api/user/delete",
		"/api/user/{ownerId}/info",
		"/api/user/{ownerId}/posts",
		"/api/user/{ownerId}/review",
		"/api/user/randomNickname"
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

		// 기본 설정인 Session 방식은 사용하지 않고 JWT 방식을 사용하기 위한 설정
		// 시큐리티 최신문서 찾아보기(아직안찾아봄)
		// http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		http.csrf().disable()	;	//csrf 비활성화

			http.authorizeHttpRequests(request -> request
				.dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()
				.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
				.requestMatchers(PERMIT_URL_ARRAY).permitAll()
				.requestMatchers("/status", "/images/**").permitAll()
				.anyRequest()
				.authenticated()

			);
		http.cors();

		http.addFilterBefore(jwtUtil, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		//접근할수있는 포트설정
		configuration.addAllowedOrigin("http://localhost:3000");
		configuration.addAllowedOrigin("http://localhost:8080");
		configuration.addAllowedOrigin("http://127.0.0.1:3000");

		configuration.addExposedHeader(JwtUtil.ACCESS_KEY);
		configuration.addExposedHeader(JwtUtil.REFRESH_KEY);
		//어떤데이터
		configuration.addAllowedHeader("*");
		//모든 방식(GET, POST, PUT, DELETE 등)으로 데이터를 요청할 수 있게함
		configuration.addAllowedMethod("*");

		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		//이 부분은 위에서 설정한 CORS 설정을 모든 경로에 적용
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}







