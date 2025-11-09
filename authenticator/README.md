# Authenticator

JWT 토큰 발급 및 사용자 인증 관리를 담당하는 Spring Boot 애플리케이션

## 개요

- **Port**: 8090
- **Database**: H2 (개발) / MariaDB (운영)
- **API 문서**: http://localhost:8090/docs (Swagger UI)

JWT 기반 인증 시스템으로 사용자 회원가입, 로그인, 토큰 발급/갱신, 계정 관리(관리자) 기능을 제공합니다.

## 주요 기능

**사용자 인증**

- 회원가입 (PENDING 상태, 관리자 승인 필요)
- 로그인 (JWT Access/Refresh Token 발급)
- 로그아웃 (Refresh Token 무효화)
- 토큰 갱신 (Token Rotation)
- 비밀번호 변경

**계정 관리 (관리자 전용)**

- 계정 CRUD 및 검색
- 계정 상태 관리 (PENDING/ACTIVE/INACTIVE)
- 계정 잠금/해제
- 비밀번호 재설정

**보안**

- BCrypt 비밀번호 해싱
- 5회 로그인 실패 시 계정 자동 잠금
- 비밀번호 변경 시 모든 토큰 무효화
- 요청 메타데이터 추적 (IP, User-Agent)

**감사 추적**

- JPA Auditing (모든 DB 변경 자동 기록)
- AccountHistory (비즈니스 이벤트 기록)

## 빠른 시작

### 1. Docker Compose로 실행 (권장)

```bash
cd authenticator
cp .env.example .env
vi .env  # JWT_SECRET_KEY 설정 필수

# MariaDB + Authenticator 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f authenticator

# 종료
docker-compose down
```

**.env 필수 설정:**

```bash
JWT_SECRET_KEY=<openssl rand -base64 64로 생성한 키>
DB_PASSWORD=your_db_password
```

### 2. Gradle로 실행 (개발)

```bash
# 프로젝트 루트에서 실행 (H2 in-memory 사용)
./gradlew :authenticator:bootRun

# - API: http://localhost:8090
# - Swagger UI: http://localhost:8090/docs
# - H2 Console: http://localhost:8090/h2-console
```

### 4. 동작 확인

**Health Check**

```bash
curl http://localhost:8090/actuator/health
```

**테스트 계정 (개발 환경)**

개발 환경에서는 다음 테스트 계정이 자동 생성됩니다:

| Username  | Password  | Role       | Status  |
| --------- | --------- | ---------- | ------- |
| `admin`   | `admin`   | ROLE_ADMIN | ACTIVE  |
| `user`    | `user`    | ROLE_USER  | ACTIVE  |
| `pending` | `pending` | ROLE_USER  | PENDING |

**로그인 테스트**

```bash
curl -X POST http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

**인증 API 호출**

```bash
export ACCESS_TOKEN="<받은 accessToken>"
curl -X GET http://localhost:8090/api/accounts \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

## 환경 설정

### 필수 환경 변수 (운영)

```bash
export JWT_SECRET_KEY="your-base64-encoded-secret-key"  # 필수
export DB_URL="jdbc:mariadb://localhost:3306/auth_db"
export DB_USERNAME="auth_user"
export DB_PASSWORD="your-password"
```

### JWT Secret Key 생성

```bash
# OpenSSL로 생성 (64바이트)
openssl rand -base64 64

# 또는 Python
python3 -c "import os, base64; print(base64.b64encode(os.urandom(64)).decode())"
```

### 설정 커스터마이징

**application.yml** (JAR 실행 시 같은 디렉토리에 배치)

```yaml
server:
  port: 9000

hanju:
  jwt:
    validator:
      secret-key: ${JWT_SECRET_KEY}
      access-token-expire-minutes: 720 # 12시간
      refresh-token-multiplier: 20 # Refresh: 240시간
```

### 프로파일

**개발 환경** (기본, H2 사용)

```bash
./gradlew :authenticator:bootRun
```

**운영 환경** (MariaDB 필수)

```bash
java -jar authenticator.jar --spring.profiles.active=prod
```

## 데이터베이스

### H2 (개발)

- **URL**: jdbc:h2:mem:authdb
- **Console**: http://localhost:8090/h2-console
- **Username**: sa
- **Password**: (공백)

### MariaDB (운영)

**1. 데이터베이스 생성**

```sql
CREATE DATABASE auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'auth_user'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON auth_db.* TO 'auth_user'@'%';
FLUSH PRIVILEGES;
```

**2. 환경 변수 설정 후 실행**

```bash
export DB_URL="jdbc:mariadb://localhost:3306/auth_db"
export DB_USERNAME="auth_user"
export DB_PASSWORD="your_password"
java -jar authenticator.jar --spring.profiles.active=prod
```

## API 엔드포인트

### 인증 (/api/auth)

- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/logout` - 로그아웃
- `POST /api/auth/refresh` - 토큰 갱신
- `PUT /api/auth/change-password` - 비밀번호 변경

### 계정 관리 (/api/accounts) - 관리자 전용

- `GET /api/accounts` - 계정 목록 조회 (검색 필터 지원)
- `POST /api/accounts` - 계정 생성
- `GET /api/accounts/{publicId}` - 계정 조회
- `PUT /api/accounts/{publicId}` - 계정 수정
- `DELETE /api/accounts/{publicId}` - 계정 삭제
- `PUT /api/accounts/{publicId}/status` - 상태 변경
- `POST /api/accounts/{publicId}/lock` - 계정 잠금
- `POST /api/accounts/{publicId}/unlock` - 계정 잠금 해제
- `PUT /api/accounts/{publicId}/reset-password` - 비밀번호 재설정

**자세한 API 명세는 Swagger UI 참고**: http://localhost:8090/docs

## 관련 문서

- [프로젝트 루트 README](../README.md) - 프로젝트 개요 및 빌드 방법
- [Validator 라이브러리 README](../validator/README.md) - JWT 검증 라이브러리 사용법

---

Copyright (c) 2025 Hanju.
