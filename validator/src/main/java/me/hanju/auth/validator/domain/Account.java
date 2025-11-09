package me.hanju.auth.validator.domain;

import java.io.Serializable;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import me.hanju.auth.validator.enums.AccountRole;
import me.hanju.auth.validator.enums.AccountStatus;

/** 계정 사용자 도메인(JPA 엔티티가 아닌 POJO) */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account implements Serializable {
  private static final long serialVersionUID = 1L;

  /** 사용자 공개 식별자 */
  private String publicId;

  /** 로그인용 ID */
  private String username;

  /** 계정 권한 */
  private AccountRole role;

  /** 마지막 로그인 시간 */
  private Instant lastLoginAt;

  /** 계정 상태 */
  @Builder.Default
  private AccountStatus status = AccountStatus.PENDING;

  /** 계정 잠금 여부 */
  @Builder.Default
  private boolean locked = false;

  /** 계정 생성 시간 */
  private Instant createdAt;
}
