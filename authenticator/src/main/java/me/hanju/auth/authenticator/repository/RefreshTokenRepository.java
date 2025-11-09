package me.hanju.auth.authenticator.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import me.hanju.auth.authenticator.entity.AccountEntity;
import me.hanju.auth.authenticator.entity.RefreshTokenEntity;

/** Refresh Token Repository */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

  /**
   * 토큰으로 조회
   *
   * @param token 토큰 문자열
   * @return RefreshToken 엔티티 (Optional)
   */
  Optional<RefreshTokenEntity> findByToken(String token);

  /**
   * 계정의 모든 Refresh Token 조회
   *
   * @param account 계정 엔티티
   * @return RefreshToken 목록
   */
  List<RefreshTokenEntity> findByAccount(AccountEntity account);

  /**
   * 계정의 유효한 Refresh Token 조회
   *
   * @param account 계정 엔티티
   * @param now 현재 시간
   * @return 유효한 RefreshToken 목록
   */
  List<RefreshTokenEntity> findByAccountAndRevokedFalseAndExpiresAtGreaterThan(AccountEntity account, Instant now);

  /**
   * 토큰 존재 여부 확인
   *
   * @param token 토큰 문자열
   * @return 존재 여부
   */
  boolean existsByToken(String token);

  /**
   * 계정의 모든 토큰 폐기 (bulk update)
   *
   * @param account 계정 엔티티
   * @param revokedAt 폐기 시간
   * @param reason 폐기 사유
   */
  @Modifying
  @Query("UPDATE RefreshTokenEntity rt SET rt.revoked = true, rt.revokedAt = :revokedAt, rt.revokedReason = :reason WHERE rt.account = :account")
  void revokeAllTokensByAccount(@Param("account") AccountEntity account,
      @Param("revokedAt") Instant revokedAt,
      @Param("reason") String reason);

  /**
   * IP 주소로 토큰 조회
   *
   * @param ipAddress IP 주소
   * @return RefreshToken 목록
   */
  List<RefreshTokenEntity> findByIpAddress(String ipAddress);

  /**
   * 계정과 User Agent로 토큰 조회 (폐기되지 않은 토큰만)
   *
   * @param account 계정 엔티티
   * @param userAgent User-Agent 문자열
   * @return RefreshToken 엔티티 (Optional)
   */
  Optional<RefreshTokenEntity> findByAccountAndUserAgentAndRevokedFalse(AccountEntity account, String userAgent);
}
