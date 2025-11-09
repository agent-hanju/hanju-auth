package me.hanju.auth.validator.domain;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.hanju.auth.validator.enums.AccountRole;

/** JWT 토큰 정보 DTO */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TokenInfo {

  /** Access Token */
  private String accessToken;

  /** Refresh Token */
  private String refreshToken;

  /** 토큰 타입 (Bearer) */
  @Builder.Default
  private String tokenType = "Bearer";

  /** Access Token 만료 시간 (초) */
  private Long expiresIn;

  /** 사용자 이름 */
  private String username;

  /** 사용자 권한 */
  private AccountRole role;

  /** 토큰 발급 시간 */
  private Instant issuedAt;
}
