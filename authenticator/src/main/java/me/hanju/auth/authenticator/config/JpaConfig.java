package me.hanju.auth.authenticator.config;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import me.hanju.auth.validator.domain.Account;

/**
 * JPA 설정
 *
 * <p>
 * JPA Auditing을 통해 createdBy, lastModifiedBy 필드를 자동으로 채움
 * (JWT 토큰에서 현재 사용자의 publicId를 추출하여 사용)
 * </p>
 */
@Configuration
@EntityScan(basePackages = "me.hanju.auth.authenticator.entity")
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableJpaRepositories(basePackages = "me.hanju.auth.authenticator.repository")
@EnableTransactionManagement
@ConditionalOnProperty(prefix = "hanju.authenticator", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JpaConfig {

  /**
   * 현재 사용자의 publicId를 제공하는 AuditorAware Bean
   *
   * <p>
   * Spring Security Context에서 인증된 사용자의 publicId를 추출
   * </p>
   *
   * @return AuditorAware 현재 사용자의 publicId를 반환
   */
  @Bean
  public AuditorAware<String> auditorProvider() {
    return () -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      if (authentication == null ||
          !authentication.isAuthenticated() ||
          "anonymousUser".equals(authentication.getPrincipal())) {
        // 인증되지 않은 경우 또는 익명 사용자인 경우 "system" 반환
        // 회원가입 등 인증 전 작업에서 사용됨
        return Optional.of("system");
      }

      // JWT 인증 후 principal에는 Account 객체가 저장됨
      // JwtAuthenticationFilter에서 JwtAuthenticationToken(account, ...) 생성
      Object principal = authentication.getPrincipal();

      if (principal instanceof Account account) {
        return Optional.of(account.getPublicId());
      }

      return Optional.of("system");
    };
  }
}
