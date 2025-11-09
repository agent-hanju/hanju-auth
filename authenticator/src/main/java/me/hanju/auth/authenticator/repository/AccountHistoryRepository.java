package me.hanju.auth.authenticator.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import me.hanju.auth.authenticator.entity.AccountHistoryEntity;
import me.hanju.auth.authenticator.entity.AccountHistoryEntity.AccountEventType;

/** 계정 이력 Repository */
@Repository
public interface AccountHistoryRepository extends JpaRepository<AccountHistoryEntity, Long> {

  /**
   * 특정 계정의 이력 조회
   *
   * @param accountPublicId 계정 공개 ID
   * @param pageable 페이징 정보
   * @return 계정 이력 페이지
   */
  Page<AccountHistoryEntity> findByAccountPublicId(String accountPublicId, Pageable pageable);

  /**
   * 특정 계정의 특정 이벤트 타입 이력 조회
   *
   * @param accountPublicId 계정 공개 ID
   * @param eventType 이벤트 타입
   * @param pageable 페이징 정보
   * @return 계정 이력 페이지
   */
  Page<AccountHistoryEntity> findByAccountPublicIdAndEventType(String accountPublicId, AccountEventType eventType,
      Pageable pageable);

  /**
   * 특정 계정의 이력 조회 (기간 범위)
   *
   * @param accountPublicId 계정 공개 ID
   * @param from            시작 시간 (inclusive)
   * @param to              종료 시간 (inclusive)
   * @return 계정 이력 목록
   */
  List<AccountHistoryEntity> findByAccountPublicIdAndCreatedAtBetweenOrderByCreatedAtDesc(
      String accountPublicId, Instant from, Instant to);

  /**
   * 특정 이벤트 타입의 이력 조회
   *
   * @param eventType 이벤트 타입
   * @param pageable 페이징 정보
   * @return 계정 이력 페이지
   */
  Page<AccountHistoryEntity> findByEventType(AccountEventType eventType, Pageable pageable);

  /**
   * 실패한 이벤트 이력 조회
   *
   * @param success 성공 여부
   * @param pageable 페이징 정보
   * @return 계정 이력 페이지
   */
  Page<AccountHistoryEntity> findBySuccessOrderByCreatedAtDesc(Boolean success, Pageable pageable);

  /**
   * 특정 IP의 이력 조회
   *
   * @param ipAddress IP 주소
   * @param pageable 페이징 정보
   * @return 계정 이력 페이지
   */
  Page<AccountHistoryEntity> findByIpAddress(String ipAddress, Pageable pageable);

  /**
   * 최근 로그인 이력 조회
   *
   * @param eventType 이벤트 타입
   * @param pageable 페이징 정보
   * @return 계정 이력 페이지
   */
  Page<AccountHistoryEntity> findByEventTypeOrderByCreatedAtDesc(AccountEventType eventType, Pageable pageable);
}
