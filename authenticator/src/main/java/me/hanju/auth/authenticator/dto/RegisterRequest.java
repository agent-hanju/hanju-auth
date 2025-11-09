package me.hanju.auth.authenticator.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 회원가입 요청 DTO (일반 사용자용) */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원가입 요청")
public class RegisterRequest {

  @NotBlank(message = "아이디는 필수입니다")
  @Schema(description = "사용자명 (로그인 ID)", example = "user123", requiredMode = RequiredMode.REQUIRED)
  private String username;

  @NotBlank(message = "비밀번호는 필수입니다")
  @Schema(description = "비밀번호 (최소 8자 권장)", example = "password123", requiredMode = RequiredMode.REQUIRED)
  private String password;

  /**
   * 사용자 권한 (API에서 설정하므로 외부에서 제공 불가)
   * 항상 ROLE_USER로 고정됨
   */
  @JsonIgnore
  @Schema(hidden = true)
  private String role; // 무시됨 - 항상 ROLE_USER로 설정
}
