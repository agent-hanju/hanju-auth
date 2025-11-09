# Validator

[Authenticator](authenticator/README.md)를 위한 JWT 검증 라이브러리

## 개요

Validator는 Authenticator에서 발급된 JWT 토큰을 검증하기 위한 경량 라이브러리입니다.
Spring Boot Auto-Configuration을 지원하여 최소한의 설정만으로 JWT 인증 메서드를 프로젝트에 추가할 수 있습니다.

## 라이브러리 정보

```
Repository: JitPack (https://jitpack.io)
Group ID: com.github.agent-hanju.hanju-auth
Artifact ID: validator
Version: v0.1.0 (Git tag)
Packaging: jar (thin jar)
```

## 주요 기능

- JWT 토큰 생성 및 검증 (HMAC-SHA512)
- Spring Security 자동 통합
- Servlet Filter 기반 토큰 추출 및 검증
- URL 패턴 기반 인증 설정
- Spring Boot Auto-Configuration 지원

## 사용 가이드

### 1. 의존성 추가

#### Gradle

```gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.agent-hanju.hanju-auth:validator:v0.1.0'
}
```

#### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.agent-hanju.hanju-auth</groupId>
        <artifactId>validator</artifactId>
        <version>v0.1.0</version>
    </dependency>
</dependencies>
```

### 2. 설정 파일 작성

**application.yml**

```yaml
hanju:
  jwt:
    validator:
      enabled: true
      secret-key: ${JWT_SECRET_KEY} # 환경 변수 (Authenticator와 동일 필수)
      issuer: hanju-auth #  JWT 발급자(Authenticator와 동일 필수)
      access-token-expire-minutes: 1440 # 24시간
      refresh-token-multiplier: 10 # Refresh 토큰은 Access의 10배 (240시간)
```

**중요**: Authenticator의 설정과 **동일한 JWT_SECRET_KEY**를 공유해야 합니다!

### 3. Security 설정

**SecurityConfig.java**

```java
package com.example.myservice.config;

import me.hanju.auth.validator.autoconfigure.JwtValidatorConfigurer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtValidatorConfigurer jwtValidatorConfigurer;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // JWT Validator 설정 적용 (JWT 필터, CSRF, Session 관리)
        jwtValidatorConfigurer.configure(http);

        // 추가 인증 규칙 정의
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated())
            .build();
    }
}
```

### 4. 환경 변수 설정

```bash
# JWT_SECRET_KEY는 Authenticator 서버와 동일한 값을 사용해야 합니다!
export JWT_SECRET_KEY="your-base64-encoded-secret-key"
```

## 사용법

### 컨트롤러에서 계정 정보 접근

```java
import me.hanju.auth.validator.domain.Account;
import me.hanju.auth.validator.domain.JwtAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class MyController {

    @GetMapping("/me")
    public ResponseEntity<UserInfo> getCurrentUser(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Account account = jwtAuth.getAccount();

            return ResponseEntity.ok(UserInfo.builder()
                .publicId(account.getPublicId())
                .username(account.getUsername())
                .role(account.getRole())
                .build());
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/admin/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Account>> getAllAccounts() {
        return ResponseEntity.ok(accountService.findAll());
    }
}
```

## API 레퍼런스

### Account 도메인 모델

```java
@Getter
@Builder
public class Account {
    private String publicId;           // 계정 공개 ID (Nano ID)
    private String username;           // 사용자명 (로그인 ID)
    private AccountRole role;          // 권한
    private Instant lastLoginAt;       // 마지막 로그인 시각
    private AccountStatus status;      // 계정 상태 (PENDING/ACTIVE/INACTIVE)
    private boolean locked;            // 계정 잠금 여부
    private Instant createdAt;         // 생성 시각
}
```

### AccountRole Enum

```java
@Getter
@RequiredArgsConstructor
public enum AccountRole {
    ROLE_ADMIN("ROLE_ADMIN", "시스템 관리자"),
    ROLE_USER("ROLE_USER", "일반 사용자"),
    ROLE_GUEST("ROLE_GUEST", "임시 사용자"),    // 현재 사용하지 않음
    ROLE_REFRESH("ROLE_REFRESH", "리프레시 토큰");

    private final String authority;
    private final String description;
}
```

### AccountStatus Enum

```java
public enum AccountStatus {
    PENDING,    // 수락 대기 상태
    ACTIVE,     // 활성화 상태
    INACTIVE    // 비활성화 상태
}
```

### JwtTokenType Enum

```java
public enum JwtTokenType {
    ACCESS_TOKEN,        // Access 토큰 (기본 만료 시간)
    REFRESH_TOKEN,       // Refresh 토큰 (기본 만료 시간 × multiplier)
    REFRESH_TOKEN_LONG   // Remember me Refresh 토큰 (기본 만료 시간 × multiplier²)
}
```

## 설정 옵션

| 속성                          | 타입    | 기본값     | 설명                      |
| ----------------------------- | ------- | ---------- | ------------------------- |
| `enabled`                     | Boolean | true       | Auto-Configuration 활성화 |
| `secret-key`                  | String  | -          | JWT 서명 키 (**필수**)    |
| `issuer`                      | String  | hanju-auth | JWT issuer claim          |
| `access-token-expire-minutes` | Integer | 1440       | Access Token 만료 시간    |
| `refresh-token-multiplier`    | Integer | 10         | Refresh Token 배수        |

---

Copyright (c) 2025 Hanju.
