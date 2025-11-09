package me.hanju.auth.authenticator.entity;

import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import me.hanju.auth.authenticator.config.MapConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 계정 이력 엔티티
 * 모든 계정 관련 작업의 이력을 추적합니다.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "account_history", indexes = {
    @Index(name = "idx_account_history_public_id", columnList = "account_public_id"),
    @Index(name = "idx_account_history_event_type", columnList = "event_type"),
    @Index(name = "idx_account_history_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class AccountHistoryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "history_id")
  @Comment("이력 ID (Primary Key)")
  private Long id;

  @Column(name = "account_public_id", length = 10, nullable = false)
  @Comment("계정 공개 ID (FK 없는 느슨한 연관관계)")
  private String accountPublicId;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", length = 50, nullable = false)
  @Comment("이벤트 타입")
  private AccountEventType eventType;

  @Column(name = "ip_address", length = 45)
  @Comment("IP 주소")
  private String ipAddress;

  @Column(name = "user_agent", length = 500)
  @Comment("User Agent")
  private String userAgent;

  @Convert(converter = MapConverter.class)
  @Column(name = "details", length = 2000)
  @Comment("상세 정보 (JSON 형식)")
  private Map<String, Object> details;

  @Column(name = "success", nullable = false)
  @Comment("성공 여부")
  @Builder.Default
  private boolean success = true;

  @Column(name = "performed_by", length = 10)
  @Comment("수행자 (publicId)")
  private String performedBy;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  @Comment("생성 일시")
  private Instant createdAt;

  /**
   * 계정 이벤트 타입 Enum
   */
  public enum AccountEventType {
    /** 로그인 시도 */
    LOGIN_ATTEMPT,

    /** 로그인 성공 */
    LOGIN_SUCCESS,

    /** 로그아웃 */
    LOGOUT,

    /** 비밀번호 변경 */
    PASSWORD_CHANGE,

    /** 토큰 재발급 */
    TOKEN_REFRESH,

    /** 계정 생성 */
    ACCOUNT_CREATE,

    /** 계정 수정 */
    ACCOUNT_UPDATE,

    /** 계정 상태 변경 */
    ACCOUNT_STATUS_CHANGE,

    /** 계정 삭제 (Real Delete) */
    ACCOUNT_DELETE,

    /** 계정 잠금 */
    ACCOUNT_LOCK,

    /** 계정 잠금 해제 */
    ACCOUNT_UNLOCK,

    /** 비밀번호 재설정 (관리자) */
    PASSWORD_RESET,

    /** 회원가입 */
    REGISTER
  }
}
