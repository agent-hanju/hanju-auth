package me.hanju.auth.authenticator.dto;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;

import me.hanju.auth.validator.enums.AccountRole;
import me.hanju.auth.validator.enums.AccountStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 계정 검색 필터 DTO
 * <p>
 * 모든 필드는 optional이며, 제공된 필드만 AND 조건으로 필터링됨
 * </p>
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "계정 검색 필터 (모든 필드 optional, AND 조건)")
public class AccountSearchRequest {

  /** 키워드 검색 (username에 대해 부분 일치) */
  @Schema(name = "q", description = "키워드 검색 (username 부분 일치)", example = "admin")
  private String keyword;

  /** 사용자 권한 필터 */
  @Schema(description = "권한 필터", example = "ROLE_ADMIN")
  private AccountRole role;

  /**
   * 계정 상태 필터
   * <p>
   * PENDING: 수락 대기, ACTIVE: 활성화, INACTIVE: 비활성화, null: 전체
   * </p>
   */
  @Schema(description = "계정 상태 필터 (PENDING/ACTIVE/INACTIVE, null: 전체)", example = "ACTIVE")
  private AccountStatus status;

  /**
   * 잠금 상태 필터
   * <p>
   * true: 잠긴 계정만, false: 잠기지 않은 계정만, null: 전체
   * </p>
   */
  @Schema(description = "잠금 상태 필터 (true: 잠김, false: 잠기지 않음, null: 전체)", example = "false")
  private Boolean locked;

  /** 생성일 범위 검색 - 시작 (inclusive) */
  @Schema(description = "생성일 범위 검색 시작 (ISO-8601)", example = "2024-01-01T00:00:00Z")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Instant createdFrom;

  /** 생성일 범위 검색 - 종료 (exclusive) */
  @Schema(description = "생성일 범위 검색 종료 (ISO-8601)", example = "2024-12-31T23:59:59Z")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Instant createdTo;

  /** 마지막 로그인 범위 검색 - 시작 (inclusive) */
  @Schema(description = "마지막 로그인 범위 시작 (ISO-8601)", example = "2024-01-01T00:00:00Z")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Instant lastLoginFrom;

  /** 마지막 로그인 범위 검색 - 종료 (exclusive) */
  @Schema(description = "마지막 로그인 범위 종료 (ISO-8601)", example = "2024-12-31T23:59:59Z")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Instant lastLoginTo;
}
