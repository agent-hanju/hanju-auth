package me.hanju.auth.validator.domain;

import java.util.Collection;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/** JWT 기반 인증 토큰(Spring Security의 Authentication 구현체) */
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = false)
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

  private static final long serialVersionUID = 1L;

  private final Account principal;
  private final String credentials;

  /**
   * 인증되지 않은 토큰 생성자.(계정 정보도, role도 없음)
   *
   * @param token 토큰 문자열(credentials로 저장)
   */
  public JwtAuthenticationToken(final String token) {
    super(null);
    this.principal = null;
    this.credentials = token;
    this.setAuthenticated(false);
  }

  /**
   * 인증된 토큰 생성자
   *
   * @param principal   Account 객체(principal로 저장)
   * @param token       토큰 문자열(credentials로 저장)
   * @param authorities AccountRole 목록
   */
  public JwtAuthenticationToken(
      final Account principal,
      final String token,
      final Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    this.credentials = token;
    this.setAuthenticated(true);
  }

  @Override
  public Object getCredentials() {
    return this.credentials;
  }

  @Override
  public Object getPrincipal() {
    return this.principal;
  }

  /**
   * Account 타입으로 principal 반환
   *
   * @return Account 객체
   */
  public Account getAccount() {
    return this.principal;
  }

  /**
   * 토큰 문자열로 credentials 반환
   *
   * @return JWT 토큰 문자열
   */
  public String getToken() {
    return this.credentials;
  }
}
