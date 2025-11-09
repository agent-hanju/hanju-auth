package me.hanju.auth.validator.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import me.hanju.auth.validator.config.JwtValidatorProperties;
import me.hanju.auth.validator.service.JwtTokenService;

/**
 * JWT Validator 자동 설정
 *
 * <p>
 * 이 라이브러리는 2개의 public Bean을 제공합니다:
 * </p>
 * <ul>
 * <li>{@link JwtTokenService}: JWT 토큰 생성/검증 서비스</li>
 * <li>{@link JwtValidatorConfigurer}: Spring Security 통합 설정</li>
 * </ul>
 *
 * application.yml 설정 예시:
 *
 * <pre>
 * hanju:
 *   jwt:
 *     validator:
 *       enabled: true
 *       secret-key: your-secret-key-base64-encoded
 *       issuer: hanju-auth
 * </pre>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(HttpSecurity.class)
@ConditionalOnProperty(prefix = "hanju.jwt.validator", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JwtValidatorProperties.class)
public class JwtValidatorAutoConfiguration {

  /**
   * JwtTokenService Bean 생성 (Public API)
   *
   * <p>
   * JwtTokenService는 secretKey를 생성자에서 특별한 로직으로 초기화하므로 @Bean 메서드로 명시적으로 생성합니다.
   * </p>
   * <p>
   * 이 Bean은 애플리케이션에서 직접 주입받아 토큰 생성/검증에 사용할 수 있습니다.
   * </p>
   *
   * @param properties
   * @return
   */
  @Bean
  @ConditionalOnMissingBean
  public JwtTokenService jwtTokenService(final JwtValidatorProperties properties) {
    return new JwtTokenService(properties);
  }

  /**
   * JwtValidatorConfigurer Bean 생성 (Public API)
   *
   * <p>
   * 이 Bean은 Spring Security HttpSecurity 설정에 JWT 검증 필터를 추가하는데 사용됩니다.
   * </p>
   *
   * @param jwtTokenService
   * @param properties
   * @return
   */
  @Bean
  @ConditionalOnMissingBean
  public JwtValidatorConfigurer jwtValidatorConfigurer(
      final JwtTokenService jwtTokenService,
      final JwtValidatorProperties properties) {
    return new JwtValidatorConfigurer(new JwtAuthenticationFilter(jwtTokenService, properties), properties);
  }
}
