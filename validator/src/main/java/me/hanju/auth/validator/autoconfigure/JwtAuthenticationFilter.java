package me.hanju.auth.validator.autoconfigure;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hanju.auth.validator.config.JwtValidatorProperties;
import me.hanju.auth.validator.domain.Account;
import me.hanju.auth.validator.domain.JwtAuthenticationToken;
import me.hanju.auth.validator.service.JwtTokenService;

/**
 * JWT 인증 필터
 * SecurityFilterChain에 추가되어 JWT 토큰을 검증하고 인증 정보를 설정
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenService jwtTokenService;
  private final JwtValidatorProperties properties;

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain) throws ServletException, IOException {

    try {
      final String token = this.resolveTokenNullable(request);

      if (token != null && !token.isBlank() && jwtTokenService.validate(token)) {
        final Account account = jwtTokenService.getAccount(token);
        final List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(account.getRole().getAuthority()));
        final JwtAuthenticationToken authentication = new JwtAuthenticationToken(
            account,
            token,
            authorities);

        authentication.setDetails(
            new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("JWT authentication successful for account: {}", account.getUsername());
      }
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      log.error("JWT authentication failed: {}", e.getMessage());
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.getWriter().write("Authentication failed: " + e.getMessage());
    }
  }

  /**
   * Request에서 토큰 추출
   *
   * @param request
   * @return
   */
  private String resolveTokenNullable(final HttpServletRequest request) {
    final String bearerToken = request.getHeader(properties.getTokenHeader());

    if (bearerToken != null
        && !bearerToken.isBlank()
        && bearerToken.startsWith(properties.getTokenPrefix())) {
      return bearerToken.substring(properties.getTokenPrefix().length());
    } else {
      return null;
    }
  }
}
