package me.hanju.auth.authenticator.exception;

/**
 * 계정 중복 예외
 * <p>
 * 이미 존재하는 사용자명으로 계정 생성을 시도할 때 발생합니다.
 * </p>
 */
public class AccountAlreadyExistsException extends RuntimeException {

  /**
   * 메시지를 포함한 예외 생성
   *
   * @param message 예외 메시지
   */
  public AccountAlreadyExistsException(String message) {
    super(message);
  }

  /**
   * 메시지와 원인을 포함한 예외 생성
   *
   * @param message 예외 메시지
   * @param cause 원인 예외
   */
  public AccountAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
