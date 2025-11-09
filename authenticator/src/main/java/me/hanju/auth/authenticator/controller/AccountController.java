package me.hanju.auth.authenticator.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.hanju.auth.authenticator.dto.AccountCreateRequest;
import me.hanju.auth.authenticator.dto.AccountResponse;
import me.hanju.auth.authenticator.dto.AccountSearchRequest;
import me.hanju.auth.authenticator.dto.AccountStatusChangeRequest;
import me.hanju.auth.authenticator.dto.AccountUpdateRequest;
import me.hanju.auth.authenticator.dto.ResetPasswordRequest;
import me.hanju.auth.authenticator.service.AccountService;
import me.hanju.auth.validator.domain.Account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 계정 관리 컨트롤러 (관리자 전용) */
@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "계정 관리 API (관리자 전용)")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AccountController {

  private final AccountService service;

  /**
   * 계정 통합 검색 API (동적 필터링)
   * <p>
   * 모든 필터 조건은 AND로 결합되며, null인 조건은 무시됩니다.
   * </p>
   *
   * <p>
   * 사용 예시:
   * </p>
   * <ul>
   * <li>전체 목록 조회: GET /api/accounts (필터 없음)</li>
   * <li>키워드 검색: GET /api/accounts?keyword=admin</li>
   * <li>권한 필터: GET /api/accounts?role=ROLE_ADMIN</li>
   * <li>활성 계정만: GET /api/accounts?enabled=true&amp;locked=false</li>
   * <li>복합 필터: GET /api/accounts?role=ROLE_USER&amp;enabled=true&amp;keyword=test</li>
   * <li>날짜 범위: GET
   * /api/accounts?createdFrom=2024-01-01T00:00:00Z&amp;createdTo=2024-12-31T23:59:59Z</li>
   * </ul>
   *
   * @param currentUser 현재 인증된 사용자
   * @param filter   검색 필터 (모든 필드 optional, Query Parameter로 전달)
   * @param pageable 페이징 정보
   * @return 필터링된 계정 목록
   */
  @GetMapping
  @Operation(summary = "계정 통합 검색", description = "동적 필터링을 지원하는 통합 검색 API. 모든 필터는 AND 조건이며, 제공되지 않은 필터는 무시됩니다.")
  public ResponseEntity<Page<AccountResponse>> searchAccounts(
      @AuthenticationPrincipal Account currentUser,
      @ModelAttribute AccountSearchRequest filter,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    Page<AccountResponse> accounts = service.searchAccounts(filter, pageable);
    return ResponseEntity.ok(accounts);
  }

  /**
   * 사용자 상세 조회 (공개 ID)
   *
   * @param currentUser 현재 인증된 사용자
   * @param publicId 조회할 계정의 공개 ID
   * @return 계정 상세 정보
   */
  @GetMapping("/{publicId}")
  @Operation(summary = "사용자 상세 조회", description = "공개 ID로 특정 사용자의 상세 정보를 조회합니다")
  public ResponseEntity<AccountResponse> getUserByPublicId(
      @AuthenticationPrincipal Account currentUser,
      @PathVariable("publicId") String publicId) {
    AccountResponse user = service.getAccountByPublicId(publicId);
    return ResponseEntity.ok(user);
  }

  /**
   * 계정 생성
   *
   * @param currentUser 현재 인증된 사용자 (관리자)
   * @param request 계정 생성 요청 정보
   * @return 생성된 계정 정보
   */
  @PostMapping
  @Operation(summary = "계정 생성", description = "새로운 계정을 생성합니다")
  public ResponseEntity<AccountResponse> createAccount(
      @AuthenticationPrincipal Account currentUser,
      @Valid @RequestBody AccountCreateRequest request) {
    AccountResponse user = service.createAccount(request, currentUser.getPublicId());
    return ResponseEntity.status(HttpStatus.CREATED).body(user);
  }

  /**
   * 계정 수정
   *
   * @param currentUser 현재 인증된 사용자 (관리자)
   * @param publicId 수정할 계정의 공개 ID
   * @param request 계정 수정 요청 정보
   * @return 수정된 계정 정보
   */
  @PutMapping("/{publicId}")
  @Operation(summary = "계정 수정", description = "기존 계정을 수정합니다(비밀번호 제외)")
  public ResponseEntity<AccountResponse> updateAccount(
      @AuthenticationPrincipal Account currentUser,
      @PathVariable("publicId") String publicId,
      @Valid @RequestBody AccountUpdateRequest request) {
    AccountResponse user = service.updateAccount(publicId, request, currentUser.getPublicId());
    return ResponseEntity.ok(user);
  }

  /**
   * 계정 상태 변경
   *
   * @param currentUser 현재 인증된 사용자 (관리자)
   * @param publicId 상태를 변경할 계정의 공개 ID
   * @param request 상태 변경 요청 정보
   * @return 변경된 계정 정보
   */
  @PutMapping("/{publicId}/status")
  @Operation(summary = "계정 상태 변경", description = "계정의 상태를 변경합니다 (PENDING, ACTIVE, INACTIVE)")
  public ResponseEntity<AccountResponse> changeAccountStatus(
      @AuthenticationPrincipal Account currentUser,
      @PathVariable("publicId") String publicId,
      @Valid @RequestBody AccountStatusChangeRequest request) {
    AccountResponse user = service.changeAccountStatus(publicId, request, currentUser.getPublicId());
    return ResponseEntity.ok(user);
  }

  /**
   * 계정 영구 삭제 (Real Delete)
   *
   * @param currentUser 현재 인증된 사용자 (관리자)
   * @param publicId 삭제할 계정의 공개 ID
   * @return 응답 없음 (204 No Content)
   */
  @DeleteMapping("/{publicId}/permanent")
  @Operation(summary = "계정 영구 삭제", description = "계정을 데이터베이스에서 영구적으로 삭제합니다 (복구 불가능)")
  public ResponseEntity<Void> deleteAccountPermanently(
      @AuthenticationPrincipal Account currentUser,
      @PathVariable("publicId") String publicId) {
    service.deleteAccountPermanently(publicId, currentUser.getPublicId());
    return ResponseEntity.noContent().build();
  }

  /**
   * 계정 잠금
   *
   * @param currentUser 현재 인증된 사용자 (관리자)
   * @param publicId 잠금할 계정의 공개 ID
   * @return 응답 없음 (204 No Content)
   */
  @PostMapping("/{publicId}/lock")
  @Operation(summary = "계정 잠금", description = "계정을 잠급니다")
  public ResponseEntity<Void> lockAccount(
      @AuthenticationPrincipal Account currentUser,
      @PathVariable("publicId") String publicId) {
    service.lockAccount(publicId, currentUser.getPublicId());
    return ResponseEntity.noContent().build();
  }

  /**
   * 계정 잠금 해제
   *
   * @param currentUser 현재 인증된 사용자 (관리자)
   * @param publicId 잠금 해제할 계정의 공개 ID
   * @return 응답 없음 (204 No Content)
   */
  @PostMapping("/{publicId}/unlock")
  @Operation(summary = "계정 잠금 해제", description = "잠긴 계정을 해제합니다")
  public ResponseEntity<Void> unlockAccount(
      @AuthenticationPrincipal Account currentUser,
      @PathVariable("publicId") String publicId) {
    service.unlockAccount(publicId, currentUser.getPublicId());
    return ResponseEntity.noContent().build();
  }

  /**
   * 비밀번호 재설정
   *
   * @param currentUser 현재 인증된 사용자 (관리자)
   * @param publicId 비밀번호를 재설정할 계정의 공개 ID
   * @param request 비밀번호 재설정 요청 정보
   * @return 응답 없음 (204 No Content)
   */
  @PostMapping("/{publicId}/reset-password")
  @Operation(summary = "비밀번호 재설정", description = "계정의 비밀번호를 재설정합니다")
  public ResponseEntity<Void> resetPassword(
      @AuthenticationPrincipal Account currentUser,
      @PathVariable("publicId") String publicId,
      @Valid @RequestBody ResetPasswordRequest request) {
    service.resetPassword(publicId, request.getNewPassword(), currentUser.getPublicId());
    return ResponseEntity.noContent().build();
  }
}
