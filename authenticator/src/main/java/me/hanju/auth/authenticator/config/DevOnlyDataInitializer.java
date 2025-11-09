package me.hanju.auth.authenticator.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hanju.auth.authenticator.entity.AccountEntity;
import me.hanju.auth.authenticator.repository.AccountRepository;
import me.hanju.auth.validator.enums.AccountRole;
import me.hanju.auth.validator.enums.AccountStatus;

/**
 * 개발 환경용 초기 데이터 로더
 * <p>
 * 애플리케이션 시작 시 기본 계정들을 자동으로 생성합니다.
 * 운영 환경(prod 프로필)에서는 실행되지 않습니다.
 * </p>
 */
@Slf4j
@Component
@Profile("!prod") // 운영 환경에서는 실행 안 됨
@RequiredArgsConstructor
public class DevOnlyDataInitializer implements ApplicationRunner {

  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    log.debug("=== Starting Data Initialization ===");

    // 1. 시스템 관리자 계정 생성
    createAccountIfNotExists(
        "admin",
        "admin",
        AccountRole.ROLE_ADMIN,
        AccountStatus.ACTIVE);

    // 2. 일반 사용자 계정 생성 (테스트용)
    createAccountIfNotExists(
        "user",
        "user",
        AccountRole.ROLE_USER,
        AccountStatus.ACTIVE);

    // 3. 승인 대기 중인 사용자 생성 (테스트용)
    createAccountIfNotExists(
        "pending",
        "pending",
        AccountRole.ROLE_USER,
        AccountStatus.PENDING);

    log.debug("=== Data Initialization Completed ===");
  }

  /**
   * 계정이 존재하지 않으면 생성
   *
   * @param username 사용자명
   * @param password 비밀번호 (평문)
   * @param role 계정 권한
   * @param status 계정 상태
   */
  private void createAccountIfNotExists(
      final String username,
      final String password,
      final AccountRole role,
      final AccountStatus status) {

    if (!accountRepository.existsByUsername(username)) {
      accountRepository.save(AccountEntity.builder()
          .username(username)
          .password(passwordEncoder.encode(password))
          .role(role)
          .status(status)
          .build());
      log.debug("✓ Created account: {} (role: {}, status: {})",
          username, role, status);
    } else {
      log.debug("Account already exists: {}", username);
    }
  }
}
