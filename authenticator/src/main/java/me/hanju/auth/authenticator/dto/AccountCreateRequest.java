package me.hanju.auth.authenticator.dto;

import me.hanju.auth.validator.enums.AccountRole;
import me.hanju.auth.validator.enums.AccountStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 관리자용 계정 생성 요청 DTO */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountCreateRequest {

  @NotBlank(message = "아이디는 필수입니다")
  private String username;

  @NotBlank(message = "비밀번호는 필수입니다")
  private String password;

  @Builder.Default
  private AccountRole role = AccountRole.ROLE_USER;

  /** 계정 상태 (기본값: ACTIVE) */
  private AccountStatus status;
}
