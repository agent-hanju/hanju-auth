package me.hanju.auth.authenticator.service;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.hanju.auth.authenticator.dto.LoginRequest;
import me.hanju.auth.authenticator.dto.RegisterRequest;
import me.hanju.auth.authenticator.dto.RequestMetadata;
import me.hanju.auth.authenticator.entity.AccountEntity;
import me.hanju.auth.authenticator.entity.AccountHistoryEntity.AccountEventType;
import me.hanju.auth.authenticator.entity.RefreshTokenEntity;
import me.hanju.auth.authenticator.exception.AccountAlreadyExistsException;
import me.hanju.auth.authenticator.exception.AuthenticationException;
import me.hanju.auth.authenticator.repository.AccountRepository;
import me.hanju.auth.authenticator.repository.RefreshTokenRepository;
import me.hanju.auth.validator.domain.Account;
import me.hanju.auth.validator.domain.TokenInfo;
import me.hanju.auth.validator.enums.AccountStatus;
import me.hanju.auth.validator.service.JwtTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증 서비스
 * 로그인, 회원가입, 토큰 갱신 등을 처리
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthenticationService {

  private final AccountRepository accountRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final AccountHistoryService accountHistoryService;
  private final JwtTokenService jwtTokenService;
  private final PasswordEncoder passwordEncoder;

  /**
   * 회원가입 (일반 사용자)
   * - Role은 항상 ROLE_USER로 고정
   * - Status는 PENDING (관리자 승인 대기)
   *
   * @param request 회원가입 요청 정보
   * @return 생성된 계정 정보
   */
  public Account register(final RegisterRequest request) {
    // 사용자명 중복 확인
    if (accountRepository.existsByUsername(request.getUsername())) {
      throw new AccountAlreadyExistsException("이미 존재하는 아이디입니다: " + request.getUsername());
    }

    // 계정 엔티티 저장 (항상 ROLE_USER, PENDING 상태)
    final AccountEntity userEntity = accountRepository.save(AccountEntity.builder()
        .username(request.getUsername())
        .password(passwordEncoder.encode(request.getPassword()))
        .role(me.hanju.auth.validator.enums.AccountRole.ROLE_USER) // 강제 고정
        .status(AccountStatus.PENDING) // 관리자 승인 대기
        .build());

    // 이력 기록
    Map<String, Object> details = new HashMap<>();
    details.put("role", "ROLE_USER");
    details.put("status", "PENDING");
    accountHistoryService.logHistory(userEntity, AccountEventType.REGISTER, true, details, userEntity.getPublicId());

    return userEntity.toDomain();
  }

  /**
   * 로그인
   *
   * <p>
   * noRollbackFor를 사용하여 AuthenticationException 발생 시에도 트랜젝션은 성공함
   * </p>
   *
   * @param request  로그인 요청 (username, password, rememberMe)
   * @param metadata HTTP 요청 메타데이터 (IP, User-Agent 등)
   * @return TokenInfo (Access Token + Refresh Token)
   */
  @Transactional(noRollbackFor = AuthenticationException.class)
  public TokenInfo login(final LoginRequest request, final RequestMetadata metadata) {
    // 사용자 조회
    final AccountEntity userEntity = accountRepository.findByUsername(request.getUsername())
        .orElseThrow(() -> new AuthenticationException("사용자를 찾을 수 없습니다"));

    // 계정 상태 확인
    if (userEntity.getStatus() == AccountStatus.INACTIVE) {
      Map<String, Object> details = new HashMap<>();
      details.put("reason", "account_inactive");
      accountHistoryService.logHistory(userEntity, AccountEventType.LOGIN_ATTEMPT, false, details,
          userEntity.getPublicId(), metadata.getIpAddress(), metadata.getUserAgent());
      throw new AuthenticationException("비활성화된 계정입니다");
    }

    if (userEntity.getStatus() == AccountStatus.PENDING) {
      Map<String, Object> details = new HashMap<>();
      details.put("reason", "account_pending");
      accountHistoryService.logHistory(userEntity, AccountEventType.LOGIN_ATTEMPT, false, details,
          userEntity.getPublicId(), metadata.getIpAddress(), metadata.getUserAgent());
      throw new AuthenticationException("계정이 아직 승인되지 않았습니다. 관리자에게 문의하세요");
    }

    if (userEntity.isLocked()) {
      Map<String, Object> details = new HashMap<>();
      details.put("reason", "account_locked");
      accountHistoryService.logHistory(userEntity, AccountEventType.LOGIN_ATTEMPT, false, details,
          userEntity.getPublicId(), metadata.getIpAddress(), metadata.getUserAgent());
      throw new AuthenticationException("잠긴 계정입니다. 관리자에게 문의하세요");
    }

    // 비밀번호 확인
    if (!passwordEncoder.matches(request.getPassword(), userEntity.getPassword())) {
      // 실패 카운트 증가 및 계정 잠금 처리
      userEntity.increasePasswordErrorCount();
      accountRepository.save(userEntity);

      Map<String, Object> details = new HashMap<>();
      details.put("reason", "wrong_password");
      details.put("password_error_count", userEntity.getPasswordErrorCount());
      accountHistoryService.logHistory(userEntity, AccountEventType.LOGIN_ATTEMPT, false, details,
          userEntity.getPublicId(), metadata.getIpAddress(), metadata.getUserAgent());

      // 카운트가 5 이상이면 계정 잠금 메시지
      if (userEntity.getPasswordErrorCount() >= 5) {
        throw new AuthenticationException("비밀번호를 5회 이상 틀려 계정이 잠겼습니다");
      }

      throw new AuthenticationException("비밀번호가 일치하지 않습니다");
    }

    // 로그인 성공 처리
    userEntity.onLoginSuccess();
    accountRepository.save(userEntity);

    // 로그인 성공 이력 기록
    Map<String, Object> details = new HashMap<>();
    details.put("remember_me", request.isRememberMe());
    accountHistoryService.logHistory(userEntity, AccountEventType.LOGIN_SUCCESS, true, details,
        userEntity.getPublicId(), metadata.getIpAddress(), metadata.getUserAgent());

    // 기존 동일 User Agent의 토큰이 있으면 폐기
    final String userAgent = metadata.getUserAgent();
    if (userAgent != null) {
      refreshTokenRepository.findByAccountAndUserAgentAndRevokedFalse(userEntity, userAgent)
          .ifPresent(token -> token.revoke("새로운 로그인"));
    }

    // 토큰 생성
    final Account authUser = userEntity.toDomain();
    final TokenInfo tokenInfo = jwtTokenService.createTokenInfo(authUser, request.isRememberMe());

    // Refresh Token 저장
    final Date expiresAt = jwtTokenService.getExpiration(tokenInfo.getRefreshToken());
    final RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
        .token(tokenInfo.getRefreshToken())
        .account(userEntity)
        .expiresAt(expiresAt.toInstant())
        .ipAddress(metadata.getIpAddress())
        .userAgent(userAgent)
        .rememberMe(request.isRememberMe())
        .build();
    refreshTokenRepository.save(refreshToken);

    return tokenInfo;
  }

  /**
   * 로그아웃
   *
   * @param refreshToken Refresh Token
   * @param performedBy  수행자 publicId
   * @param metadata     HTTP 요청 메타데이터 (IP, User-Agent 등)
   */
  public void logout(final String refreshToken, final String performedBy, final RequestMetadata metadata) {
    refreshTokenRepository.findByToken(refreshToken)
        .ifPresent(token -> {
          token.revoke("로그아웃");
          refreshTokenRepository.save(token);

          // 로그아웃 이력 기록
          final AccountEntity userEntity = token.getAccount();
          accountHistoryService.logHistory(userEntity, AccountEventType.LOGOUT, true, new HashMap<>(),
              performedBy, metadata.getIpAddress(), metadata.getUserAgent());
        });
  }

  /**
   * 토큰 갱신 (Refresh Token Rotation 방식)
   *
   * 보안 강화를 위해 refresh할 때마다 새로운 refresh token을 발급하고
   * 기존 refresh token은 폐기합니다.
   *
   * @param refreshToken Refresh Token
   * @param metadata     HTTP 요청 메타데이터 (IP, User-Agent 등)
   * @return TokenInfo (새로운 Access Token + 새로운 Refresh Token + username + role)
   */
  public TokenInfo refresh(String refreshToken, RequestMetadata metadata) {
    // Refresh Token 조회
    final RefreshTokenEntity tokenEntity = refreshTokenRepository.findByToken(refreshToken)
        .orElseThrow(() -> new AuthenticationException("유효하지 않은 Refresh Token입니다"));

    // 토큰 유효성 확인
    if (!tokenEntity.isValid()) {
      throw new AuthenticationException("만료되었거나 폐기된 토큰입니다");
    }

    // 토큰 검증
    if (!jwtTokenService.validate(refreshToken)) {
      throw new AuthenticationException("토큰 검증에 실패했습니다");
    }

    // 계정 정보 확인
    final AccountEntity accountEntity = tokenEntity.getAccount();
    if (!accountEntity.isActive() || accountEntity.isLocked()) {
      throw new AuthenticationException("계정이 비활성화되었거나 잠겨있습니다");
    }

    // rememberMe 여부 확인 (기존 토큰의 rememberMe 필드 사용)
    final boolean rememberMe = tokenEntity.isRememberMe();

    // 새로운 토큰 쌍 생성 (Access Token + Refresh Token)
    final Account account = accountEntity.toDomain();
    final TokenInfo newTokenInfo = jwtTokenService.createTokenInfo(account, rememberMe);

    // 기존 Refresh Token 폐기
    tokenEntity.revoke("새로운 토큰으로 교체");
    refreshTokenRepository.save(tokenEntity);

    // 새로운 Refresh Token 저장
    final Date expiresAt = jwtTokenService.getExpiration(newTokenInfo.getRefreshToken());
    final RefreshTokenEntity newRefreshToken = RefreshTokenEntity.builder()
        .token(newTokenInfo.getRefreshToken())
        .account(accountEntity)
        .expiresAt(expiresAt.toInstant())
        .ipAddress(metadata.getIpAddress())
        .userAgent(metadata.getUserAgent())
        .rememberMe(rememberMe)
        .build();
    refreshTokenRepository.save(newRefreshToken);

    final String performedBy = account.getPublicId();
    log.info("Token refreshed with rotation for user [{}] from IP: {}, User-Agent: {}",
        performedBy, metadata.getIpAddress(), metadata.getUserAgent());

    return newTokenInfo;
  }

  /**
   * 비밀번호 변경
   *
   * @param publicId        계정 public ID
   * @param currentPassword 현재 비밀번호
   * @param newPassword     새 비밀번호
   * @param metadata        HTTP 요청 메타데이터 (IP, User-Agent 등)
   */
  public void changePassword(String publicId, String currentPassword, String newPassword, RequestMetadata metadata) {
    final AccountEntity accountEntity = accountRepository.findByPublicId(publicId)
        .orElseThrow(() -> new AuthenticationException("사용자를 찾을 수 없습니다"));

    // 현재 비밀번호 확인
    if (!passwordEncoder.matches(currentPassword, accountEntity.getPassword())) {
      // 실패 이력 기록
      Map<String, Object> details = new HashMap<>();
      details.put("reason", "current_password_mismatch");
      accountHistoryService.logHistory(accountEntity, AccountEventType.PASSWORD_CHANGE, false, details,
          accountEntity.getPublicId(), metadata.getIpAddress(), metadata.getUserAgent());
      throw new AuthenticationException("현재 비밀번호가 일치하지 않습니다");
    }

    // 비밀번호 변경
    accountEntity.setPassword(passwordEncoder.encode(newPassword));
    accountRepository.save(accountEntity);

    // 모든 Refresh Token 폐기 (보안상 재로그인 유도)
    refreshTokenRepository.revokeAllTokensByAccount(
        accountEntity,
        Instant.now(),
        "비밀번호 변경");

    // 성공 이력 기록
    Map<String, Object> details = new HashMap<>();
    details.put("tokens_revoked", true);
    accountHistoryService.logHistory(accountEntity, AccountEventType.PASSWORD_CHANGE, true, details,
        accountEntity.getPublicId(), metadata.getIpAddress(), metadata.getUserAgent());
  }
}
