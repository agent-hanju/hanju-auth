package me.hanju.auth.authenticator.dto;

import me.hanju.auth.validator.enums.AccountRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 관리자용 사용자 수정 요청 DTO */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpdateRequest {
  /** 사용자명 (선택) */
  private String username;

  /** 권한 (선택) */
  private AccountRole role;
}
