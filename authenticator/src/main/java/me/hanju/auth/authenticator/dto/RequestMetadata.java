package me.hanju.auth.authenticator.dto;

import lombok.Builder;
import lombok.Getter;

/** HTTP 요청 메타데이터 (감사 로깅용) */
@Builder
@Getter
public class RequestMetadata {

  /** 클라이언트 IP 주소 */
  private final String ipAddress;

  /** User-Agent 문자열 */
  private final String userAgent;

  /**
   * 빈 메타데이터 생성 (테스트용)
   *
   * @return 빈 RequestMetadata 객체
   */
  public static RequestMetadata empty() {
    return RequestMetadata.builder().build();
  }
}
