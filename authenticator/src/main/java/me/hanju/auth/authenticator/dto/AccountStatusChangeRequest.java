package me.hanju.auth.authenticator.dto;

import me.hanju.auth.validator.enums.AccountStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 계정 상태 변경 요청 DTO */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatusChangeRequest {

  @NotNull(message = "계정 상태는 필수입니다")
  private AccountStatus status;

  private String reason; // 상태 변경 사유 (선택)
}
