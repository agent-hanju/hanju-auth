package me.hanju.auth.authenticator.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.hanju.auth.authenticator.entity.AccountEntity;
import me.hanju.auth.authenticator.entity.AccountHistoryEntity;
import me.hanju.auth.authenticator.entity.AccountHistoryEntity.AccountEventType;
import me.hanju.auth.authenticator.repository.AccountHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 계정 이력 관리 서비스
 *
 * <p>
 * 계정 관련 모든 이벤트(로그인, 로그아웃, 계정 수정 등)의 이력을 기록하고 관리합니다.
 *
 * <p>
 * <b>책임:</b>
 * <ul>
 * <li>계정 이력 생성 및 저장</li>
 * <li>보안 감사(Audit) 추적 지원</li>
 * <li>IP 주소, User Agent 등 컨텍스트 정보 기록</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountHistoryService {

  private final AccountHistoryRepository accountHistoryRepository;

  /**
   * 계정 이력 로깅 (IP와 User Agent 포함)
   *
   * @param account     계정 엔티티
   * @param eventType   이벤트 타입
   * @param success     성공 여부
   * @param details     상세 정보 (Map)
   * @param performedBy 수행자 publicId
   * @param ipAddress   IP 주소 (선택)
   * @param userAgent   User Agent (선택)
   */
  @Transactional
  public void logHistory(final AccountEntity account, final AccountEventType eventType,
      final boolean success, final Map<String, Object> details,
      final String performedBy, final String ipAddress, final String userAgent) {

    AccountHistoryEntity history = AccountHistoryEntity.builder()
        .accountPublicId(account.getPublicId())
        .eventType(eventType)
        .success(success)
        .details(details)
        .performedBy(performedBy)
        .ipAddress(ipAddress)
        .userAgent(userAgent)
        .build();

    accountHistoryRepository.save(history);

    log.debug("Account history logged: eventType={}, accountPublicId={}, success={}",
        eventType, account.getPublicId(), success);
  }

  /**
   * 계정 이력 로깅 (IP와 User Agent 없음)
   *
   * <p>
   * 관리자 작업 등 HTTP 요청이 없는 경우 사용
   *
   * @param account     계정 엔티티
   * @param eventType   이벤트 타입
   * @param success     성공 여부
   * @param details     상세 정보 (Map)
   * @param performedBy 수행자 publicId
   */
  @Transactional
  public void logHistory(final AccountEntity account, final AccountEventType eventType,
      final boolean success, final Map<String, Object> details, final String performedBy) {
    this.logHistory(account, eventType, success, details, performedBy, null, null);
  }
}
