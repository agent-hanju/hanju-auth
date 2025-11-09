package me.hanju.auth.authenticator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 로그인 요청 DTO */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "로그인 요청")
public class LoginRequest {

  @NotBlank(message = "아이디는 필수입니다")
  @Schema(description = "사용자명", example = "user123", requiredMode = RequiredMode.REQUIRED)
  private String username;

  @NotBlank(message = "비밀번호는 필수입니다")
  @Schema(description = "비밀번호", example = "password123", requiredMode = RequiredMode.REQUIRED)
  private String password;

  /** 자동 로그인 여부 (Refresh Token 만료 시간 연장) */
  @Builder.Default
  @Schema(description = "자동 로그인 여부 (Refresh Token 수명 연장)", example = "false")
  private boolean rememberMe = false;
}
