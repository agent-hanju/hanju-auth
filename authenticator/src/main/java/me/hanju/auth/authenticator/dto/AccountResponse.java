package me.hanju.auth.authenticator.dto;

import java.time.Instant;

import me.hanju.auth.authenticator.entity.AccountEntity;
import me.hanju.auth.validator.domain.Account;
import me.hanju.auth.validator.enums.AccountRole;
import me.hanju.auth.validator.enums.AccountStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 계정 응답 DTO */
@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AccountResponse {

  private String publicId;
  private String username;
  private AccountRole role;
  private Instant lastLoginAt;
  private int passwordErrorCount;
  private AccountStatus status;
  private boolean locked;
  private Instant createdAt;
  private String createdBy;
  private Instant lastModifiedAt;
  private String lastModifiedBy;

  /**
   * Entity를 DTO로 변환
   *
   * @param entity 변환할 AccountEntity
   * @return AccountResponse DTO
   */
  public static AccountResponse from(AccountEntity entity) {
    return AccountResponse.builder()
        .publicId(entity.getPublicId())
        .username(entity.getUsername())
        .role(entity.getRole())
        .lastLoginAt(entity.getLastLoginAt())
        .passwordErrorCount(entity.getPasswordErrorCount())
        .status(entity.getStatus())
        .locked(entity.isLocked())
        .createdAt(entity.getCreatedAt())
        .createdBy(entity.getCreatedBy())
        .lastModifiedAt(entity.getLastModifiedAt())
        .lastModifiedBy(entity.getLastModifiedBy())
        .build();
  }

  /**
   * Domain model을 DTO로 변환
   *
   * @param account 변환할 Account domain model
   * @return AccountResponse DTO
   */
  public static AccountResponse from(Account account) {
    return AccountResponse.builder()
        .publicId(account.getPublicId())
        .username(account.getUsername())
        .role(account.getRole())
        .lastLoginAt(account.getLastLoginAt())
        .status(account.getStatus())
        .locked(account.isLocked())
        .createdAt(account.getCreatedAt())
        .build();
  }
}
