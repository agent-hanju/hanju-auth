package me.hanju.auth.authenticator.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import me.hanju.auth.validator.autoconfigure.JwtValidatorConfigurer;

import lombok.RequiredArgsConstructor;

/** 보안 설정, JWT Validator를 사용하여 인증 필터 추가 처리 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "hanju.authenticator", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

  private final JwtValidatorConfigurer jwtValidatorConfigurer;

  /**
   * 비밀번호 인코더
   *
   * @return BCrypt 비밀번호 인코더
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Security Filter Chain
   *
   * @param http HttpSecurity 설정 객체
   * @return SecurityFilterChain
   * @throws Exception 설정 중 예외 발생 시
   */
  @Bean
  public SecurityFilterChain authenticatorSecurityFilterChain(HttpSecurity http) throws Exception {
    // JWT Validator 설정 적용 (JWT 필터, CSRF, Session 관리)
    jwtValidatorConfigurer.configure(http);

    return http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(
                // 인증 관련 엔드포인트 (인증 불필요)
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/refresh",
                "/api/auth/forgot-password",
                "/api/auth/reset-password",
                // Actuator 엔드포인트
                "/actuator/health",
                // Swagger UI
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/docs/**",
                // H2 Console (개발용)
                "/h2-console/**",
                // 정적 리소스
                "/",
                "/error",
                "/favicon.ico",
                "/static/**",
                "/public/**")
            .permitAll()
            // 나머지 모든 요청은 인증 필요
            .anyRequest().authenticated())
        // H2 Console을 위한 frame options 비활성화 (개발 환경용)
        .headers(headers -> headers
            .frameOptions(frameOptions -> frameOptions.disable()))

        .build();
  }
}
