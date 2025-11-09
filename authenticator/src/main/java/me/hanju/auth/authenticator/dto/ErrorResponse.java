package me.hanju.auth.authenticator.dto;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** API 오류 응답 DTO */
@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

  /** HTTP 상태 코드 */
  private int status;

  /** 오류 메시지 */
  private String message;

  /** 오류 발생 시각 */
  @Builder.Default
  private Instant timestamp = Instant.now();
}
