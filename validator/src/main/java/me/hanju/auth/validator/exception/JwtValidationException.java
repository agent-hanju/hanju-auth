package me.hanju.auth.validator.exception;

/** JWT 검증 예외 */
public class JwtValidationException extends RuntimeException {

  /**
   * 메시지를 포함한 예외 생성
   *
   * @param message 예외 메시지
   */
  public JwtValidationException(String message) {
    super(message);
  }

  /**
   * 메시지와 원인을 포함한 예외 생성
   *
   * @param message 예외 메시지
   * @param cause   원인 예외
   */
  public JwtValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
