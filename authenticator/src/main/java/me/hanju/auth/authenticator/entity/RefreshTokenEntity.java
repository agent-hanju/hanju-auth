package me.hanju.auth.authenticator.entity;

import java.time.Instant;

import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Refresh Token 엔티티
 * 사용자별 Refresh Token 관리
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refresh_token", indexes = {
    @Index(name = "idx_refresh_token_token", columnList = "token", unique = true),
    @Index(name = "idx_refresh_token_account", columnList = "account_id"),
    @Index(name = "idx_refresh_token_expires", columnList = "expires_at")
})
@EntityListeners(AuditingEntityListener.class)
public class RefreshTokenEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "token_id")
  @Comment("토큰 ID (Primary Key)")
  private Long id;

  @Column(name = "token", length = 500, nullable = false, unique = true)
  @Comment("Refresh Token 값")
  private String token;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false)
  @JsonBackReference
  @Comment("계정 ID (Foreign Key)")
  private AccountEntity account;

  @Column(name = "expires_at", nullable = false)
  @Comment("만료 일시")
  private Instant expiresAt;

  @Column(name = "revoked", nullable = false)
  @Comment("폐기 여부")
  @Builder.Default
  private boolean revoked = false;

  @Column(name = "revoked_at")
  @Comment("폐기 일시")
  private Instant revokedAt;

  @Column(name = "revoked_reason", length = 200)
  @Comment("폐기 사유")
  private String revokedReason;

  @Column(name = "ip_address", length = 45)
  @Comment("IP 주소")
  private String ipAddress;

  @Column(name = "user_agent", length = 500)
  @Comment("User Agent")
  private String userAgent;

  @Column(name = "remember_me", nullable = false)
  @Comment("로그인 유지 여부 (Remember Me)")
  @Builder.Default
  private boolean rememberMe = false;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  @Comment("생성 일시")
  private Instant createdAt;

  @Column(name = "last_used_at")
  @Comment("마지막 사용 일시")
  private Instant lastUsedAt;

  /**
   * 토큰 폐기 처리
   *
   * @param reason 폐기 사유
   */
  public void revoke(String reason) {
    this.revoked = true;
    this.revokedAt = Instant.now();
    this.revokedReason = reason;
  }

  /**
   * 토큰 만료 여부 확인
   *
   * @return 만료 여부
   */
  public boolean isExpired() {
    return Instant.now().isAfter(this.expiresAt);
  }

  /**
   * 토큰 유효성 확인
   *
   * @return 유효성 여부 (폐기되지 않고 만료되지 않음)
   */
  public boolean isValid() {
    return !this.revoked && !isExpired();
  }

  /** 토큰 사용 기록 업데이트 */
  public void updateLastUsed() {
    this.lastUsedAt = Instant.now();
  }
}
