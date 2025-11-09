package me.hanju.auth.validator.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 사용자 권한 열거형 */
@Getter
@RequiredArgsConstructor
public enum AccountRole {
  ROLE_ADMIN("ROLE_ADMIN", "시스템 관리자"),
  ROLE_USER("ROLE_USER", "일반 사용자"),
  ROLE_GUEST("ROLE_GUEST", "임시 사용자"), // 현재 사용하지 않음
  ROLE_REFRESH("ROLE_REFRESH", "리프레시 토큰");

  private final String authority;
  private final String description;

  /**
   * 권한 문자열로 AuthRole 찾기
   *
   * @param authority
   * @return
   */
  public static AccountRole fromAuthority(final String authority) {
    for (AccountRole role : values()) {
      if (role.authority.equals(authority)) {
        return role;
      }
    }
    throw new IllegalArgumentException("Unknown authority: " + authority);
  }
}
