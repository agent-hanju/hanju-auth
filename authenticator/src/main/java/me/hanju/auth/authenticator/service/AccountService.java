package me.hanju.auth.authenticator.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import me.hanju.auth.authenticator.dto.AccountCreateRequest;
import me.hanju.auth.authenticator.dto.AccountResponse;
import me.hanju.auth.authenticator.dto.AccountSearchRequest;
import me.hanju.auth.authenticator.dto.AccountStatusChangeRequest;
import me.hanju.auth.authenticator.dto.AccountUpdateRequest;
import me.hanju.auth.authenticator.entity.AccountEntity;
import me.hanju.auth.authenticator.entity.AccountHistoryEntity.AccountEventType;
import me.hanju.auth.authenticator.exception.AccountAlreadyExistsException;
import me.hanju.auth.authenticator.exception.AuthenticationException;
import me.hanju.auth.authenticator.repository.AccountRepository;
import me.hanju.auth.authenticator.repository.RefreshTokenRepository;
import me.hanju.auth.validator.enums.AccountStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 계정 관리 서비스 (관리자용) */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

  private static final String USER_NOT_FOUND_MESSAGE = "계정을 찾을 수 없습니다: ";
  private static final String USER_ALREADY_EXISTS = "이미 존재하는 아이디입니다: ";

  private final AccountRepository accountRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final AccountHistoryService accountHistoryService;
  private final PasswordEncoder passwordEncoder;

  /**
   * 통합 검색 API (동적 필터링)
   * <p>
   * 모든 필터 조건은 AND로 결합됩니다. null인 조건은 무시됩니다.
   * </p>
   *
   * @param filter   검색 필터 (모든 필드 optional)
   * @param pageable 페이징 정보
   * @return 필터링된 계정 목록
   */
  public Page<AccountResponse> searchAccounts(final AccountSearchRequest filter, final Pageable pageable) {
    return accountRepository.searchWithFilters(
        filter.getKeyword(),
        filter.getRole(),
        filter.getStatus(),
        filter.getLocked(),
        filter.getCreatedFrom(),
        filter.getCreatedTo(),
        filter.getLastLoginFrom(),
        filter.getLastLoginTo(),
        pageable).map(AccountResponse::from);
  }

  /**
   * 계정 상세 조회 (공개 ID)
   *
   * @param publicId 조회할 계정의 공개 ID
   * @return 계정 상세 정보
   */
  public AccountResponse getAccountByPublicId(final String publicId) {
    return AccountResponse.from(accountRepository.findByPublicId(publicId)
        .orElseThrow(() -> new NoSuchElementException(USER_NOT_FOUND_MESSAGE + publicId)));
  }

  /**
   * 계정 상세 조회 (계정명)
   *
   * @param username 조회할 계정의 사용자명
   * @return 계정 상세 정보
   */
  public AccountResponse getAccountByUsername(final String username) {
    return AccountResponse.from(accountRepository.findByUsername(username)
        .orElseThrow(() -> new AuthenticationException(USER_NOT_FOUND_MESSAGE + username)));
  }

  /**
   * 계정 생성
   *
   * @param request 계정 생성 요청 정보
   * @param performedBy 수행자 publicId
   * @return 생성된 계정 정보
   */
  @Transactional
  public AccountResponse createAccount(final AccountCreateRequest request, final String performedBy) {
    // 계정명 중복 확인
    if (accountRepository.existsByUsername(request.getUsername())) {
      throw new AccountAlreadyExistsException(USER_ALREADY_EXISTS + request.getUsername());
    }

    // 계정 엔티티 생성
    final AccountEntity user = accountRepository.save(AccountEntity.builder()
        .username(request.getUsername())
        .password(passwordEncoder.encode(request.getPassword()))
        .role(request.getRole())
        .status((request.getStatus() == null) ? AccountStatus.ACTIVE : request.getStatus())
        .build());

    // 이력 기록
    Map<String, Object> details = new HashMap<>();
    details.put("role", user.getRole());
    details.put("status", user.getStatus());
    accountHistoryService.logHistory(user, AccountEventType.ACCOUNT_CREATE, true, details, performedBy);

    return AccountResponse.from(user);
  }

  /**
   * 계정 수정 (Admin) - username과 role 변경만 허용
   *
   * @param publicId    수정할 계정의 publicId
   * @param request     수정 요청
   * @param performedBy 수행자 publicId
   * @return 수정된 계정 정보
   */
  @Transactional
  public AccountResponse updateAccount(final String publicId, final AccountUpdateRequest request,
      final String performedBy) {
    final AccountEntity user = accountRepository.findByPublicId(publicId)
        .orElseThrow(() -> new NoSuchElementException(USER_NOT_FOUND_MESSAGE + publicId));

    Map<String, Object> details = new HashMap<>();

    // 계정명 변경 시 중복 확인
    if (StringUtils.hasText(request.getUsername()) && !request.getUsername().equals(user.getUsername())) {
      if (accountRepository.existsByUsername(request.getUsername())) {
        throw new AccountAlreadyExistsException(USER_ALREADY_EXISTS + request.getUsername());
      }
      String oldUsername = user.getUsername();
      user.setUsername(request.getUsername());
      details.put("username_from", oldUsername);
      details.put("username_to", request.getUsername());
    }

    // 권한 변경
    if (request.getRole() != null && request.getRole() != user.getRole()) {
      details.put("role_from", user.getRole());
      details.put("role_to", request.getRole());
      user.setRole(request.getRole());
    }

    accountRepository.save(user);

    // 이력 기록
    accountHistoryService.logHistory(user, AccountEventType.ACCOUNT_UPDATE, true, details, performedBy);

    return AccountResponse.from(user);
  }

  /**
   * 계정 상태 변경
   *
   * @param publicId    대상 계정 publicId
   * @param request     상태 변경 요청
   * @param performedBy 수행자 publicId
   * @return 변경된 계정 정보
   */
  @Transactional
  public AccountResponse changeAccountStatus(final String publicId, final AccountStatusChangeRequest request,
      final String performedBy) {
    final AccountEntity user = accountRepository.findByPublicId(publicId)
        .orElseThrow(() -> new NoSuchElementException(USER_NOT_FOUND_MESSAGE + publicId));

    AccountStatus oldStatus = user.getStatus();
    user.changeStatus(request.getStatus());
    accountRepository.save(user);

    // INACTIVE 상태로 변경 시 모든 Refresh Token 폐기
    if (request.getStatus() == AccountStatus.INACTIVE) {
      refreshTokenRepository.revokeAllTokensByAccount(user, Instant.now(), "계정 비활성화");
    }

    // 이력 기록
    Map<String, Object> details = new HashMap<>();
    details.put("status_from", oldStatus);
    details.put("status_to", request.getStatus());
    if (request.getReason() != null) {
      details.put("reason", request.getReason());
    }
    accountHistoryService.logHistory(user, AccountEventType.ACCOUNT_STATUS_CHANGE, true, details, performedBy);

    return AccountResponse.from(user);
  }

  /**
   * 계정 영구 삭제 (Real Delete)
   *
   * @param publicId    삭제할 계정 publicId
   * @param performedBy 수행자 publicId
   */
  @Transactional
  public void deleteAccountPermanently(final String publicId, final String performedBy) {
    final AccountEntity user = accountRepository.findByPublicId(publicId)
        .orElseThrow(() -> new NoSuchElementException(USER_NOT_FOUND_MESSAGE + publicId));

    // 이력 기록 (삭제 전에 먼저 기록)
    Map<String, Object> details = new HashMap<>();
    details.put("username", user.getUsername());
    accountHistoryService.logHistory(user, AccountEventType.ACCOUNT_DELETE, true, details, performedBy);

    // 영구 삭제
    accountRepository.delete(user);
  }

  /**
   * 계정 잠금
   *
   * @param publicId    잠글 계정 publicId
   * @param performedBy 수행자 publicId
   */
  @Transactional
  public void lockAccount(final String publicId, final String performedBy) {
    final AccountEntity user = accountRepository.findByPublicId(publicId)
        .orElseThrow(() -> new NoSuchElementException(USER_NOT_FOUND_MESSAGE + publicId));

    user.setLocked(true);
    accountRepository.save(user);

    // 모든 Refresh Token 폐기
    refreshTokenRepository.revokeAllTokensByAccount(user, Instant.now(), "계정 잠금");

    // 이력 기록
    accountHistoryService.logHistory(user, AccountEventType.ACCOUNT_LOCK, true, new HashMap<>(), performedBy);
  }

  /**
   * 계정 잠금 해제
   *
   * @param publicId    잠금 해제할 계정 publicId
   * @param performedBy 수행자 publicId
   */
  @Transactional
  public void unlockAccount(final String publicId, final String performedBy) {
    final AccountEntity user = accountRepository.findByPublicId(publicId)
        .orElseThrow(() -> new NoSuchElementException(USER_NOT_FOUND_MESSAGE + publicId));

    user.setLocked(false);
    user.resetPasswordErrorCount();
    accountRepository.save(user);

    // 이력 기록
    Map<String, Object> details = new HashMap<>();
    details.put("password_error_count_reset", true);
    accountHistoryService.logHistory(user, AccountEventType.ACCOUNT_UNLOCK, true, details, performedBy);
  }

  /**
   * 비밀번호 재설정 (관리자)
   *
   * @param publicId    대상 계정 publicId
   * @param newPassword 새 비밀번호
   * @param performedBy 수행자 publicId
   */
  @Transactional
  public void resetPassword(final String publicId, final String newPassword, final String performedBy) {
    final AccountEntity user = accountRepository.findByPublicId(publicId)
        .orElseThrow(() -> new NoSuchElementException(USER_NOT_FOUND_MESSAGE + publicId));

    user.setPassword(passwordEncoder.encode(newPassword));
    user.resetPasswordErrorCount();

    accountRepository.save(user);

    // 모든 Refresh Token 폐기 (보안상 재로그인 유도)
    refreshTokenRepository.revokeAllTokensByAccount(user, Instant.now(), "비밀번호 재설정");

    // 이력 기록
    Map<String, Object> details = new HashMap<>();
    details.put("tokens_revoked", true);
    accountHistoryService.logHistory(user, AccountEventType.PASSWORD_RESET, true, details, performedBy);
  }
}
