package me.hanju.auth.authenticator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 비밀번호 변경 요청 DTO */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

  @NotBlank(message = "현재 비밀번호는 필수입니다")
  private String currentPassword;

  @NotBlank(message = "새 비밀번호는 필수입니다")
  private String newPassword;

  @NotBlank(message = "새 비밀번호 확인은 필수입니다")
  private String newPasswordConfirm;
}
