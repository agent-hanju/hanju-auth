package me.hanju.auth.authenticator.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import me.hanju.auth.authenticator.entity.AccountEntity;
import me.hanju.auth.validator.enums.AccountRole;
import me.hanju.auth.validator.enums.AccountStatus;

/** 계정 Repository */
@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

  /**
   * 공개 ID로 계정 조회
   *
   * @param publicId 조회할 계정의 공개 ID
   * @return 계정 엔티티 (Optional)
   */
  Optional<AccountEntity> findByPublicId(String publicId);

  /**
   * 사용자명으로 계정 조회
   *
   * @param username 조회할 사용자명
   * @return 계정 엔티티 (Optional)
   */
  Optional<AccountEntity> findByUsername(String username);

  /**
   * 사용자명 존재 여부 확인
   *
   * @param username 확인할 사용자명
   * @return 존재 여부
   */
  boolean existsByUsername(String username);

  /**
   * 공개 ID 존재 여부 확인
   *
   * @param publicId 확인할 공개 ID
   * @return 존재 여부
   */
  boolean existsByPublicId(String publicId);

  /**
   * 특정 상태의 계정 조회
   *
   * @param status 조회할 계정 상태
   * @return 계정 엔티티 목록
   */
  List<AccountEntity> findByStatus(AccountStatus status);

  /**
   * 권한별 계정 조회
   *
   * @param role 조회할 권한
   * @param pageable 페이징 정보
   * @return 계정 엔티티 페이지
   */
  Page<AccountEntity> findByRole(AccountRole role, Pageable pageable);

  /**
   * 만료된 계정 조회 (일정 기간 동안 로그인하지 않은 계정)
   *
   * @param expiryDate 만료 기준 날짜
   * @param status 계정 상태
   * @return 만료된 계정 엔티티 목록
   */
  List<AccountEntity> findByLastLoginAtLessThanAndStatus(Instant expiryDate, AccountStatus status);

  /**
   * 특정 상태의 잠긴 계정 조회
   *
   * @param locked 잠금 상태
   * @param status 계정 상태
   * @return 계정 엔티티 목록
   */
  List<AccountEntity> findByLockedAndStatus(Boolean locked, AccountStatus status);

  /**
   * 특정 상태의 계정 페이징 조회
   *
   * @param status 조회할 계정 상태
   * @param pageable 페이징 정보
   * @return 계정 엔티티 페이지
   */
  Page<AccountEntity> findByStatus(AccountStatus status, Pageable pageable);

  /**
   * 특정 상태의 계정 수 조회
   *
   * @param status 조회할 계정 상태
   * @return 계정 수
   */
  Long countByStatus(AccountStatus status);

  /**
   * 권한별 및 상태별 계정 수 조회
   *
   * @param role 권한
   * @param status 계정 상태
   * @return 계정 수
   */
  Long countByRoleAndStatus(AccountRole role, AccountStatus status);

  /**
   * 동적 필터링 검색 (모든 조건 AND)
   * <p>
   * 모든 파라미터는 nullable이며, null인 경우 해당 조건을 무시합니다.
   * </p>
   *
   * @param keyword       키워드 (username 부분 일치, nullable)
   * @param role          권한 필터 (nullable)
   * @param status        계정 상태 필터 (nullable)
   * @param locked        잠금 상태 필터 (nullable)
   * @param createdFrom   생성일 범위 시작 (nullable, inclusive)
   * @param createdTo     생성일 범위 종료 (nullable, exclusive)
   * @param lastLoginFrom 마지막 로그인 범위 시작 (nullable, inclusive)
   * @param lastLoginTo   마지막 로그인 범위 종료 (nullable, exclusive)
   * @param pageable      페이징 정보
   * @return 필터링된 계정 목록
   */
  @Query("SELECT a FROM AccountEntity a WHERE " +
      "(:keyword IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
      "(:role IS NULL OR a.role = :role) AND " +
      "(:status IS NULL OR a.status = :status) AND " +
      "(:locked IS NULL OR a.locked = :locked) AND " +
      "(:createdFrom IS NULL OR a.createdAt >= :createdFrom) AND " +
      "(:createdTo IS NULL OR a.createdAt < :createdTo) AND " +
      "(:lastLoginFrom IS NULL OR a.lastLoginAt >= :lastLoginFrom) AND " +
      "(:lastLoginTo IS NULL OR a.lastLoginAt < :lastLoginTo)")
  Page<AccountEntity> searchWithFilters(
      @Param("keyword") String keyword,
      @Param("role") AccountRole role,
      @Param("status") AccountStatus status,
      @Param("locked") Boolean locked,
      @Param("createdFrom") Instant createdFrom,
      @Param("createdTo") Instant createdTo,
      @Param("lastLoginFrom") Instant lastLoginFrom,
      @Param("lastLoginTo") Instant lastLoginTo,
      Pageable pageable);
}
