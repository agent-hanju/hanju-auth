package me.hanju.auth.authenticator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 인증 서버 메인 애플리케이션 */
@SpringBootApplication
public class AuthenticatorApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuthenticatorApplication.class, args);
  }
}
