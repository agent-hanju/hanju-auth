package me.hanju.auth.authenticator.entity;

import java.time.Instant;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import me.hanju.auth.authenticator.util.NanoIdGenerator;
import me.hanju.auth.validator.domain.Account;
import me.hanju.auth.validator.enums.AccountRole;
import me.hanju.auth.validator.enums.AccountStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 계정 엔티티(validator의 Account 도메인 모델과 매핑) */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "account", indexes = {
    @Index(name = "idx_account_public_id", columnList = "public_id", unique = true),
    @Index(name = "idx_account_username", columnList = "username", unique = true)
})
@DynamicInsert
@DynamicUpdate
@EntityListeners(AuditingEntityListener.class)
public class AccountEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "account_id")
  @Comment("계정 ID (Primary Key)")
  private Long id;

  @Column(name = "public_id", length = 10, nullable = false, unique = true)
  @Comment("사용자 공개 ID (Nano ID)")
  private String publicId;

  @Column(name = "username", length = 100, nullable = false, unique = true)
  @Comment("사용자명 (로그인 ID)")
  @ColumnTransformer(write = "TRIM(?)")
  private String username;

  @Column(name = "password", length = 100, nullable = false)
  @Comment("비밀번호 (BCrypt)")
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(name = "auth_role", length = 20, nullable = false)
  @Comment("계정 권한")
  private AccountRole role;

  @Column(name = "last_login_at")
  @Comment("마지막 로그인 일시")
  private Instant lastLoginAt;

  @Column(name = "password_error_count", nullable = false)
  @Comment("비밀번호 오류 횟수")
  @Builder.Default
  private int passwordErrorCount = 0;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  @Comment("계정 상태 (PENDING, ACTIVE, INACTIVE)")
  @Builder.Default
  private AccountStatus status = AccountStatus.ACTIVE;

  @Column(name = "locked", nullable = false)
  @Comment("계정 잠금 여부")
  @Builder.Default
  private boolean locked = false;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  @Comment("생성 일시")
  private Instant createdAt;

  @CreatedBy
  @Column(name = "created_by", length = 10, updatable = false)
  @Comment("생성자 (publicId)")
  private String createdBy;

  @LastModifiedDate
  @Column(name = "last_modified_at", nullable = false)
  @Comment("최종 수정 일시 (JPA Auditing - DB 레벨 모든 변경)")
  private Instant lastModifiedAt;

  @LastModifiedBy
  @Column(name = "last_modified_by", length = 10)
  @Comment("최종 수정자 (JPA Auditing - publicId)")
  private String lastModifiedBy;

  @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  @JsonManagedReference
  @Builder.Default
  private java.util.Set<RefreshTokenEntity> refreshTokens = new java.util.HashSet<>();

  /** Nano ID 생성 */
  @PrePersist
  public void prePersist() {
    if (this.publicId == null) {
      this.publicId = NanoIdGenerator.generate();
    }
  }

  /** 비밀번호 오류 횟수 증가 */
  public void increasePasswordErrorCount() {
    this.passwordErrorCount++;
    if (this.passwordErrorCount >= 5) {
      this.locked = true;
    }
  }

  /** 비밀번호 오류 횟수 초기화 */
  public void resetPasswordErrorCount() {
    this.passwordErrorCount = 0;
    this.locked = false;
  }

  /** 로그인 성공 시 처리 */
  public void onLoginSuccess() {
    this.lastLoginAt = Instant.now();
    this.resetPasswordErrorCount();
  }

  /**
   * 계정 상태 변경
   *
   * @param newStatus 새로운 계정 상태
   */
  public void changeStatus(final AccountStatus newStatus) {
    this.status = newStatus;
  }

  /**
   * 계정 수락 대기 여부 확인 (PENDING 상태)
   *
   * @return PENDING 상태 여부
   */
  public boolean isPending() {
    return this.status == AccountStatus.PENDING;
  }

  /**
   * 계정 활성화 여부 확인 (ACTIVE 상태)
   *
   * @return ACTIVE 상태 여부
   */
  public boolean isActive() {
    return this.status == AccountStatus.ACTIVE;
  }

  /**
   * 계정 비활성화 여부 확인 (INACTIVE 상태)
   *
   * @return INACTIVE 상태 여부
   */
  public boolean isInactive() {
    return this.status == AccountStatus.INACTIVE;
  }

  /**
   * Account 도메인 객체로 변환
   *
   * @return Account 도메인 객체
   */
  public Account toDomain() {
    return Account.builder()
        .publicId(this.publicId)
        .username(this.username)
        .role(this.role)
        .lastLoginAt(this.lastLoginAt)
        .status(this.status)
        .locked(this.locked)
        .createdAt(this.createdAt)
        .build();
  }

  /**
   * Account 도메인 객체로부터 업데이트
   *
   * @param account 업데이트 소스 도메인 객체
   */
  public void updateFromDomain(final Account account) {
    if (account.getUsername() != null) {
      this.username = account.getUsername();
    }
    if (account.getRole() != null) {
      this.role = account.getRole();
    }
    if (account.getStatus() != null) {
      this.status = account.getStatus();
    }
    this.locked = account.isLocked();
  }
}
