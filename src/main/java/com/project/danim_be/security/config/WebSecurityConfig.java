package com.project.danim_be.security.config;


import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

	private static final String[] PERMIT_URL_ARRAY={
		"/api/user/kakao/**",
		"/api/user/google/**",
		"/api/user/naver/**",
		"/api/user/signup",
		"/api/user/login",
		"/api/user/{ownerId}/info",
		"/api/user/{ownerId}/posts",
		"/api/user/{ownerId}/review",

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

	//h2콘솔 접근허용
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)	// 이 필터체인이 다른필터체인보다 우선순위가 높음을 표시.
	SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher(PathRequest.toH2Console());	//h2콘솔에 대한 요청만 체인을 사용한다.
		http.csrf((csrf) -> csrf.disable());				//csrf에대한 보호를 비활성한다.
		http.headers((headers) -> headers.frameOptions((frame) -> frame.sameOrigin()));
		return http.build();
	}
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		// 기본 설정인 Session 방식은 사용하지 않고 JWT 방식을 사용하기 위한 설정
		// 시큐리티 최신문서 찾아보기(아직안찾아봄)
		// http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		http
			.csrf().disable()		//csrf 비활성화
			.authorizeHttpRequests(request -> request
				.dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()
				.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
				.requestMatchers(PERMIT_URL_ARRAY).permitAll()
				.requestMatchers("/status", "/images/**").permitAll()
				.anyRequest()
				.authenticated()

			);

		// http.cors();

		// http.addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}







