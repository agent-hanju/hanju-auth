package me.hanju.auth.authenticator.util;

import me.hanju.auth.authenticator.dto.RequestMetadata;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

/**
 * HTTP 요청 메타데이터 추출 유틸리티
 *
 * IP 주소와 User-Agent 등 HTTP 요청으로부터 감사(audit) 목적의 메타데이터를 추출합니다.
 * 이러한 메타데이터는 비즈니스 데이터가 아닌 HTTP 계층 데이터이므로 DTO에 포함하지 않고
 * 필요한 시점에 추출하여 사용합니다.
 */
@UtilityClass
public class HttpRequestMetadataExtractor {
  private static final String[] CANDIDATE_HEADERS = {
      "X-Forwarded-For",
      "Proxy-Client-IP",
      "WL-Proxy-Client-IP",
      "HTTP_X_FORWARDED_FOR",
      "HTTP_X_FORWARDED",
      "HTTP_X_CLUSTER_CLIENT_IP",
      "HTTP_CLIENT_IP",
      "HTTP_FORWARDED_FOR",
      "HTTP_FORWARDED",
      "X-Real-IP"
  };

  /**
   * HTTP 요청으로부터 메타데이터 추출
   *
   * @param request HTTP 요청
   * @return RequestMetadata (IP 주소, User-Agent 포함)
   */
  public static RequestMetadata extract(HttpServletRequest request) {
    return RequestMetadata.builder()
        .ipAddress(extractClientIp(request))
        .userAgent(extractUserAgentOrNull(request))
        .build();
  }

  /**
   * 클라이언트 IP 주소 추출
   *
   * Proxy나 Load Balancer를 거쳐온 경우를 고려하여 여러 헤더를 확인합니다.
   *
   * @param request HTTP 요청
   * @return 클라이언트 IP 주소
   */
  public static String extractClientIp(HttpServletRequest request) {
    for (String header : CANDIDATE_HEADERS) {
      String ip = request.getHeader(header);
      if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
        // 여러 IP가 있는 경우 첫 번째 IP 반환 (실제 클라이언트 IP)
        if (ip.contains(",")) {
          return ip.split(",")[0].trim();
        }
        return ip;
      }
    }

    // 모든 헤더가 없으면 직접 연결된 IP 반환
    return request.getRemoteAddr();
  }

  /**
   * User-Agent 추출
   *
   * @param request HTTP 요청
   * @return User-Agent 문자열, 없으면 null
   */
  public static String extractUserAgentOrNull(HttpServletRequest request) {
    return request.getHeader("User-Agent");
  }
}
