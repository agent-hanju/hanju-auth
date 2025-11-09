package me.hanju.auth.validator.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import me.hanju.auth.validator.config.JwtValidatorProperties;
import me.hanju.auth.validator.domain.Account;
import me.hanju.auth.validator.domain.TokenInfo;
import me.hanju.auth.validator.enums.AccountRole;
import me.hanju.auth.validator.enums.JwtTokenType;
import me.hanju.auth.validator.exception.JwtValidationException;

/** JWT 토큰 생성 및 검증 서비스 */
@Slf4j
public class JwtTokenService {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final JwtValidatorProperties properties;
  private final SecretKey secretKey;

  /**
   * JwtValidatorProperties를 받아 초기화.(SecretKey도 properties에서 값을 가져와 주입한다)
   *
   * @param properties
   */
  public JwtTokenService(final JwtValidatorProperties properties) {
    this.properties = properties;
    final byte[] keyBytes = properties.getSecretKey().getBytes(StandardCharsets.UTF_8);

    // HS512는 최소 512비트(64바이트) 필요
    if (keyBytes.length < 64) {
      log.warn("JWT secret key is too short. Minimum 64 bytes required for HS512. Current: {} bytes",
          keyBytes.length);
    }

    // secretKey 초기화
    this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    log.info("JwtTokenService initialized with secret key length: {} bytes", keyBytes.length);
  }

  /** SecretKey 객체 조회 */
  private SecretKey getSecretKey() {
    return this.secretKey;
  }

  /**
   * 토큰 생성
   *
   * @param account
   * @param tokenType
   * @return
   */
  private String generateToken(final Account account, final JwtTokenType tokenType) {
    final Date now = new Date();
    final Long expireMinutes = tokenType.calculateExpireMinutes(
        properties.getAccessTokenExpireMinutes(),
        properties.getRefreshTokenMultiplier());
    final Date expiryDate = new Date(now.getTime() + (expireMinutes * 60 * 1000));

    final Map<String, Object> claims = new HashMap<>();

    // Refresh Token인 경우 권한을 ROLE_REFRESH로 설정
    final AccountRole role;
    if (tokenType == JwtTokenType.REFRESH_TOKEN || tokenType == JwtTokenType.REFRESH_TOKEN_LONG) {
      role = AccountRole.ROLE_REFRESH;
    } else {
      role = account.getRole();
    }

    claims.put(properties.getClaimNames().getRole(), role.getAuthority());
    claims.put(properties.getClaimNames().getUsername(), account.getUsername());

    return Jwts.builder()
        .claims(claims)
        .subject(account.getPublicId())
        .issuer(properties.getIssuer())
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSecretKey(), Jwts.SIG.HS512)
        .compact();
  }

  /**
   * Access Token 생성
   *
   * @param account
   * @return
   */
  public String generateAccessToken(final Account account) {
    return this.generateToken(account, JwtTokenType.ACCESS_TOKEN);
  }

  /**
   * Refresh Token 생성
   *
   * @param account
   * @param rememberMe
   * @return
   */
  public String generateRefreshToken(final Account account, final boolean rememberMe) {
    return this.generateToken(account, rememberMe ? JwtTokenType.REFRESH_TOKEN_LONG : JwtTokenType.REFRESH_TOKEN);
  }

  /**
   * 계정에 대한 현재 시간 기준 TokenInfo 생성
   *
   * @param account
   * @param rememberMe
   * @return
   */
  public TokenInfo createTokenInfo(final Account account, final boolean rememberMe) {
    final String accessToken = generateAccessToken(account);
    final String refreshToken = generateRefreshToken(account, rememberMe);

    final Long expiresIn = properties.getAccessTokenExpireMinutes() * 60; // 초 단위

    return TokenInfo.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(expiresIn)
        .username(account.getUsername())
        .role(account.getRole())
        .issuedAt(Instant.now())
        .build();
  }

  /**
   * 토큰 검증
   *
   * @param token
   * @return
   */
  public boolean validate(final String token) {
    try {
      final Claims claims = this.getClaims(token);

      // 만료 시간 검증
      if (properties.getValidationOptions().isValidateExpiration()) {
        final Date expiration = claims.getExpiration();
        if (expiration.before(new Date())) {
          log.debug("Token is expired");
          return false;
        }
      }

      // 발행자 검증
      if (properties.getValidationOptions().isValidateIssuer()) {
        String issuer = claims.getIssuer();
        if (!properties.getIssuer().equals(issuer)) {
          log.debug("Invalid issuer: {}", issuer);
          return false;
        }
      }

      return true;
    } catch (ExpiredJwtException e) {
      log.debug("Token is expired: {}", e.getMessage());
      return false;
    } catch (MalformedJwtException e) {
      log.debug("Token is malformed: {}", e.getMessage());
      return false;
    } catch (UnsupportedJwtException e) {
      log.debug("Token is unsupported: {}", e.getMessage());
      return false;
    } catch (SignatureException e) {
      log.debug("Token signature is invalid: {}", e.getMessage());
      return false;
    } catch (Exception e) {
      log.debug("Token validation failed: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 토큰에서 Claims 추출
   *
   * @param token
   * @return
   */
  public Claims getClaims(final String token) {
    return Jwts.parser()
        .verifyWith(getSecretKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  /**
   * 토큰에서 계정 public ID 추출
   */
  public String getPublicId(final String token) {
    return this.getClaims(token).getSubject();
  }

  /**
   * 토큰에서 계정 권한 추출
   *
   * @param token
   * @return
   */
  public AccountRole getRole(final String token) {
    final Claims claims = this.getClaims(token);
    final String roleString = claims.get(properties.getClaimNames().getRole(), String.class);
    return AccountRole.fromAuthority(roleString);
  }

  /**
   * 토큰에서 Account 객체 생성
   *
   * @param token
   * @return
   */
  public Account getAccount(final String token) {
    final Claims claims = this.getClaims(token);

    return Account.builder()
        .publicId(claims.getSubject())
        .username(claims.get(properties.getClaimNames().getUsername(), String.class))
        .role(this.getRole(token))
        .build();
  }

  /**
   * 토큰 만료 시간 조회
   *
   * @param token
   * @return
   */
  public Date getExpiration(final String token) {
    return this.getClaims(token).getExpiration();
  }

  /**
   * 토큰 만료 여부 확인
   *
   * @param token
   * @return
   */
  public boolean checkTokenExpired(final String token) {
    try {
      final Date expiration = this.getExpiration(token);
      return expiration.before(new Date());
    } catch (ExpiredJwtException e) {
      return true;
    }
  }

  /**
   * 토큰의 Payload를 맵으로 디코딩
   *
   * @param token
   * @return
   */
  public Map<String, Object> decodePayload(final String token) {
    final String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new JwtValidationException("Invalid JWT format");
    }
    try {
      return MAPPER.readValue(Base64.getUrlDecoder().decode(parts[1]), new TypeReference<Map<String, Object>>() {
      });
    } catch (JsonProcessingException e) {
      throw new JwtValidationException("Invalid JWT format", e);
    } catch (IOException e) {
      throw new JwtValidationException("I/O Exception occurred", e);
    }
  }

  /**
   * Bearer 토큰에서 토큰 문자열 추출
   *
   * @param bearerToken
   * @return
   */
  public String extractToken(final String bearerToken) {
    if (bearerToken != null && bearerToken.startsWith(properties.getTokenPrefix())) {
      return bearerToken.substring(properties.getTokenPrefix().length());
    }
    return bearerToken;
  }
}
