package me.hanju.auth.validator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/** JWT Validator 설정 프로퍼티 */
@Getter
@Setter
@ConfigurationProperties(prefix = "hanju.jwt.validator")
public class JwtValidatorProperties {

  /** JWT Validator 활성화 여부 */
  private boolean enabled = true;

  /** JWT Secret Key (Base64 인코딩) */
  private String secretKey;

  /** JWT 발행자 */
  private String issuer = "hanju-auth";

  /** Access Token 만료 시간 (분) */
  private Long accessTokenExpireMinutes = 1440L; // 24시간

  /** Refresh Token 만료 시간 배수 */
  private Long refreshTokenMultiplier = 10L;

  /** 토큰 헤더 이름 */
  private String tokenHeader = "Authorization";

  /** 토큰 접두사 */
  private String tokenPrefix = "Bearer ";

  /** 토큰 클레임 이름 설정 */
  private ClaimNames claimNames = new ClaimNames();

  @Getter
  @Setter
  public static class ClaimNames {
    private String role = "role";
    private String publicId = "publicId";
    private String username = "username";
  }

  /** 토큰 검증 옵션 */
  private ValidationOptions validationOptions = new ValidationOptions();

  @Getter
  @Setter
  public static class ValidationOptions {
    private boolean validateExpiration = true;
    private boolean validateIssuer = true;
    private boolean validateSignature = true;
    private Long clockSkewSeconds = 60L; // 시간 오차 허용 범위 (초)
  }
}
