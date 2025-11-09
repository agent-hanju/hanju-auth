package me.hanju.auth.validator.autoconfigure;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.hanju.auth.validator.config.JwtValidatorProperties;

/**
 * JWT Validator를 SecurityConfig에 통합하기 위한 Configurer
 *
 * 사용 예시:
 *
 * <pre>
 * {@code
 * &#64;Bean
 * public SecurityFilterChain securityFilterChain(
 *     HttpSecurity http,
 *     JwtValidatorConfigurer jwtValidatorConfigurer
 * ) throws Exception {
 *     jwtValidatorConfigurer.configure(http);
 *
 *     return http
 *         .authorizeHttpRequests(...)
 *         .build();
 * }
 * }
 * </pre>
 */
@RequiredArgsConstructor
public class JwtValidatorConfigurer {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final JwtValidatorProperties properties;

  /**
   * HttpSecurity에 JWT 검증 설정을 적용
   *
   * 이 메서드는 JWT 인증 필터와 기본 보안 설정(CSRF, Session)만 제공합니다.
   * Authorization 규칙(permitAll, authenticated 등)은 각 애플리케이션에서 직접 정의해야 합니다.
   *
   * @param http HttpSecurity 설정 객체
   * @throws Exception 설정 중 예외 발생 시
   */
  public void configure(final HttpSecurity http) throws Exception {
    if (!this.properties.isEnabled()) {
      return;
    }

    http
        // CSRF 비활성화
        .csrf(csrf -> csrf.disable())

        // 세션 관리 비활성화
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // 인증 실패 시 401 Unauthorized 응답
        .exceptionHandling(exception -> exception
            .authenticationEntryPoint((request, response, authException) -> response
                .sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))

        // JWT 필터 추가
        .addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class);
  }

  /**
   * 간편한 한 줄 설정을 위한 static 메서드
   *
   * 사용 예시:
   *
   * <pre>
   * {@code
   * @Bean
   * public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
   *   return http
   *       .apply(JwtValidatorConfigurer.jwt())
   *       .and()
   *       .build();
   * }
   * }
   * </pre>
   *
   * @return JwtValidatorDsl 객체
   */
  public static JwtValidatorDsl jwt() {
    return new JwtValidatorDsl();
  }

  /** DSL 스타일 설정을 위한 내부 클래스 */
  public static class JwtValidatorDsl extends
      AbstractHttpConfigurer<JwtValidatorDsl, HttpSecurity> {

    @Override
    public void configure(final HttpSecurity http) throws Exception {
      JwtValidatorConfigurer configurer = http
          .getSharedObject(org.springframework.context.ApplicationContext.class)
          .getBean(JwtValidatorConfigurer.class);

      configurer.configure(http);
    }
  }
}
