package me.hanju.auth.authenticator.exception;

/**
 * 인증 실패 예외
 * <p>
 * 로그인 실패, 토큰 검증 실패 등 인증 과정에서 발생하는 예외입니다.
 * </p>
 */
public class AuthenticationException extends RuntimeException {

  /**
   * 메시지를 포함한 예외 생성
   *
   * @param message 예외 메시지
   */
  public AuthenticationException(String message) {
    super(message);
  }

  /**
   * 메시지와 원인을 포함한 예외 생성
   *
   * @param message 예외 메시지
   * @param cause 원인 예외
   */
  public AuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
