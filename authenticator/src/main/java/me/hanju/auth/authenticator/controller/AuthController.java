package me.hanju.auth.authenticator.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.hanju.auth.authenticator.dto.AccountResponse;
import me.hanju.auth.authenticator.dto.ChangePasswordRequest;
import me.hanju.auth.authenticator.dto.LoginRequest;
import me.hanju.auth.authenticator.dto.RefreshRequest;
import me.hanju.auth.authenticator.dto.RegisterRequest;
import me.hanju.auth.authenticator.dto.RequestMetadata;
import me.hanju.auth.authenticator.service.AuthenticationService;
import me.hanju.auth.authenticator.util.HttpRequestMetadataExtractor;
import me.hanju.auth.validator.domain.Account;
import me.hanju.auth.validator.domain.TokenInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 인증 컨트롤러 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

  private final AuthenticationService authenticationService;

  /**
   * 회원가입
   *
   * @param request 회원가입 요청 정보
   * @return 생성된 계정 정보
   */
  @PostMapping("/register")
  @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다")
  public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {
    Account authUser = authenticationService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(authUser));
  }

  /**
   * 로그인
   *
   * @param request     로그인 요청
   * @param httpRequest HTTP 요청
   * @return TokenInfo (Access Token + Refresh Token)
   */
  @PostMapping("/login")
  @Operation(summary = "로그인", description = "사용자 인증 후 토큰을 발급합니다")
  public ResponseEntity<TokenInfo> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest) {
    // HTTP 요청 메타데이터 추출
    RequestMetadata metadata = HttpRequestMetadataExtractor.extract(httpRequest);

    TokenInfo tokenInfo = authenticationService.login(request, metadata);
    return ResponseEntity.ok(tokenInfo);
  }

  /**
   * 토큰 갱신
   *
   * @param request     Refresh Token 요청
   * @param httpRequest HTTP 요청
   * @return TokenInfo (Access Token + Refresh Token + username + role)
   */
  @PostMapping("/refresh")
  @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access Token을 발급합니다")
  public ResponseEntity<TokenInfo> refresh(
      @Valid @RequestBody RefreshRequest request,
      HttpServletRequest httpRequest) {

    // HTTP 요청 메타데이터 추출
    RequestMetadata metadata = HttpRequestMetadataExtractor.extract(httpRequest);

    TokenInfo tokenInfo = authenticationService.refresh(request.getRefreshToken(), metadata);
    return ResponseEntity.ok(tokenInfo);
  }

  /**
   * 로그아웃
   *
   * @param currentUser  현재 인증된 사용자
   * @param refreshToken Refresh Token
   * @param httpRequest  HTTP 요청
   * @return 204 No Content
   */
  @PostMapping("/logout")
  @Operation(summary = "로그아웃", description = "Refresh Token을 무효화합니다")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Void> logout(
      @AuthenticationPrincipal Account currentUser,
      @RequestHeader("Authorization") String refreshToken,
      HttpServletRequest httpRequest) {
    // Bearer 접두사 제거
    if (refreshToken.startsWith("Bearer ")) {
      refreshToken = refreshToken.substring(7);
    }

    // HTTP 요청 메타데이터 추출
    RequestMetadata metadata = HttpRequestMetadataExtractor.extract(httpRequest);

    authenticationService.logout(refreshToken, currentUser.getPublicId(), metadata);
    return ResponseEntity.noContent().build();
  }

  /**
   * 비밀번호 변경 (본인 계정)
   *
   * @param currentUser 현재 인증된 사용자
   * @param request     비밀번호 변경 요청
   * @param httpRequest HTTP 요청
   * @return 204 No Content
   */
  @PostMapping("/change-password")
  @Operation(summary = "비밀번호 변경", description = "사용자 본인의 비밀번호를 변경합니다. 현재 비밀번호 확인과 새 비밀번호 이중 확인이 필요합니다.")
  @SecurityRequirement(name = "Bearer Authentication")
  public ResponseEntity<Void> changePassword(
      @AuthenticationPrincipal Account currentUser,
      @Valid @RequestBody ChangePasswordRequest request,
      HttpServletRequest httpRequest) {

    // 새 비밀번호와 확인 비밀번호 일치 여부 확인
    if (request.getNewPassword() == null || !request.getNewPassword().equals(request.getNewPasswordConfirm())) {
      throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다");
    }

    // HTTP 요청 메타데이터 추출
    RequestMetadata metadata = HttpRequestMetadataExtractor.extract(httpRequest);

    authenticationService.changePassword(
        currentUser.getPublicId(),
        request.getCurrentPassword(),
        request.getNewPassword(),
        metadata);

    return ResponseEntity.noContent().build();
  }
}
