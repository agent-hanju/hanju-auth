package me.hanju.auth.authenticator.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 토큰 갱신 요청 DTO */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 갱신 요청")
public class RefreshRequest {

  @NotBlank(message = "Refresh Token은 필수입니다")
  @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzUxMiJ9...", requiredMode = RequiredMode.REQUIRED)
  private String refreshToken;
}
