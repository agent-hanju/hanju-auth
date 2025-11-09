# Hanju Auth

JWT 로그인 서버 / 검증 라이브러리

## 프로젝트 개요

기본적인 JWT 방식 로그인을 구현해놓은 MSA용 서버, 검증 라이브러리입니다.

### 모듈 구조

```
hanju-auth/
├── authenticator/  # 로그인 서버 (fat jar): JWT 발급 및 사용자 인증 관리
└── validator/      # JWT 검증 라이브러리 (thin jar): authenticator의 JWT 검증 메서드 제공
```

## 빠른 시작

### 필수 요구사항

- **Java**: 17 이상
- **Gradle**: 8.x (Wrapper 포함)

### 1. 프로젝트 클론 및 빌드

```bash
git clone https://github.com/agent-hanju/hanju-auth.git
cd hanju-auth
chmod +x gradlew  # Linux/Mac only

# 전체 빌드
./gradlew build
```

### 2. Authenticator 서버 실행

#### 2-1. docker compose로 실행 (권장)

```bash
cd authenticator
cp .env.example .env
vi .env  # JWT_SECRET_KEY 설정 필수

docker-compose up -d

# 로그 확인
docker-compose logs -f authenticator

# 서버 종료
docker compose down
```

#### 2-2. bootRun으로 실행 (개발)

```bash
./gradlew :authenticator:bootRun

# - API: http://localhost:8090
# - Swagger UI: http://localhost:8090/docs
# - H2 Memory DB Console: http://localhost:8090/h2-console
```

**※ 참고) 개발 환경 초기 계정**

- 개발 환경에서는 애플리케이션 시작 시 다음 테스트 계정들이 자동으로 생성됩니다 ([DevOnlyDataInitializer.java](authenticator/src/main/java/me/hanju/auth/authenticator/config/DevOnlyDataInitializer.java))
- 운영 환경(`prod`)에서는 생성되지 않습니다.

| Username  | Password  | Role       | Status  | 설명                        |
| --------- | --------- | ---------- | ------- | --------------------------- |
| `admin`   | `admin`   | ROLE_ADMIN | ACTIVE  | 시스템 관리자 계정          |
| `user`    | `user`    | ROLE_USER  | ACTIVE  | 일반 사용자 (테스트용)      |
| `pending` | `pending` | ROLE_USER  | PENDING | 승인 대기 사용자 (테스트용) |

### 3. Validator 라이브러리 사용하기

다른 프로젝트에서 validator를 사용하려면 JitPack을 통해 의존성을 추가하세요.

#### 3-1. JitPack 저장소 추가 (build.gradle)

```gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

#### 3-2. Validator 의존성 추가

```gradle
dependencies {
    implementation 'com.github.agent-hanju.hanju-auth:validator:v0.1.0'
}
```

#### 3-3. application.yml 설정

```yaml
hanju:
  jwt:
    validator:
      enabled: true
      secret-key: ${JWT_SECRET_KEY} # Base64 인코딩된 32바이트 이상 키
      issuer: hanju-auth
      access-token-expire-minutes: 1440
```

자세한 설정 방법은 [Validator README](validator/README.md)를 참고하세요.

### 4. 동작 확인

```bash
# Health check
curl http://localhost:8090/actuator/health

# 회원가입
curl -X POST http://localhost:8090/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Password123!"}'

# 로그인
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Password123!","remeberMe":true}'

# Swagger UI에서 테스트
open http://localhost:8090/docs
```

## 문서

- **[Authenticator 서버 README](authenticator/README.md)** - 인증 서버 실행 및 설정 가이드
- **[Validator 라이브러리 README](validator/README.md)** - 마이크로서비스에 JWT 검증 라이브러리 통합하기

---

Copyright (c) 2025 Hanju.
