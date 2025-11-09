# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

use context7.

**Project Name**: hanju-auth (formerly auth-library)

## MANDATORY REQUIREMENTS (필수 요구사항)

These are ABSOLUTE requirements that override any other considerations. 이 요구사항들은 다른 모든 고려사항보다 우선합니다.

### Domain Model vs Entity 분리 (HIGHEST PRIORITY)

**validator 모듈 MUST:**

- Domain model만 포함 (Account, TokenInfo, JwtAuthenticationToken)
- Enum 타입은 별도 enums 패키지에 포함 (AccountRole, AccountStatus, JwtTokenType)
- NEVER JPA 어노테이션이나 persistence 로직 포함
- Domain model은 POJO여야 하며 프레임워크 의존성 없어야 함
- ALWAYS service 메서드 시그니처와 반환 타입에 domain model 사용

**authenticator 모듈 MUST:**

- Persistence를 위해 JPA entity 사용 (AccountEntity, RefreshTokenEntity, AccountHistoryEntity)
- ALWAYS entity를 domain model로 변환 후 validator service에 전달 (.toDomain() 메서드 사용)
- NEVER controller response에서 entity 직접 노출 - DTO 사용 필수
- NEVER entity를 validator 모듈 메서드에 직접 전달

**ABSOLUTELY FORBIDDEN:**

- validator domain model에 JPA 어노테이션 추가
- JwtTokenService에서 Account 대신 AccountEntity 사용
- Service 메서드에서 domain model/DTO 대신 entity 반환
- Domain 로직과 persistence 로직 혼합

<good-example>
// authenticator의 Service 메서드
public TokenInfo login(LoginRequest request) {
    AccountEntity entity = accountRepository.findByUsername(request.getUsername());
    Account domainUser = entity.toDomain(); // Entity를 Domain model로 변환
    return jwtTokenService.generateToken(domainUser); // Domain model 전달
}

// Controller에서 DTO 반환
public AccountResponse getAccount(String publicId) {
AccountEntity entity = accountRepository.findByPublicId(publicId);
return AccountResponse.from(entity); // DTO로 변환하여 반환
}
</good-example>

<bad-example>
// FORBIDDEN: Entity를 validator service에 직접 전달
public TokenInfo login(LoginRequest request) {
    AccountEntity entity = accountRepository.findByUsername(request.getUsername());
    return jwtTokenService.generateToken(entity); // FORBIDDEN - 잘못된 타입
}

// FORBIDDEN: Controller에서 Entity 직접 반환
public AccountEntity getAccount(String publicId) { // FORBIDDEN
return accountRepository.findByPublicId(publicId);
}
</bad-example>

### 보안 구현 요구사항 (MANDATORY)

**비밀번호 처리:**

- ALWAYS PasswordEncoder(BCrypt)로 비밀번호 해싱
- NEVER 평문 비밀번호를 저장하거나 로깅
- NEVER 문자열 비교로 비밀번호 검증 - PasswordEncoder.matches() 사용 필수

**토큰 보안:**

- JWT secret key는 MUST Base64 인코딩되고 최소 256비트(32바이트)
- NEVER JWT secret key를 버전 관리에 커밋
- ALWAYS 토큰 만료 검증 후 claims 신뢰
- NEVER 명시적 요구 없이 access token 수명을 24시간 이상 연장
- ALWAYS 비밀번호 변경 시 refresh token 폐기

**계정 보안:**

- MUST 5회 로그인 실패 시 계정 잠금
- ALWAYS AccountHistoryEntity에 계정 이벤트 기록 (로그인, 생성, 업데이트, 삭제 등)
- MUST public-facing user ID로 Nano ID 사용 (database primary key 노출 금지)
- 현재 구현: Hard delete 사용 중 (soft delete는 향후 고려사항)

<good-example>
// 올바른 비밀번호 검증
if (passwordEncoder.matches(rawPassword, user.getPassword())) {
    user.resetPasswordErrorCount();
    // 로그인 성공
} else {
    user.increasePasswordErrorCount();
    if (user.getPasswordErrorCount() >= 5) {
        user.lock();
    }
}

// 올바른 계정 삭제 (감사 이력 보존)
public void deleteAccount(String publicId, String deletedBy, RequestMetadata metadata) {
    AccountEntity account = findByPublicId(publicId);
    // 삭제 전 감사 이력 기록 (AccountHistory는 FK가 아니므로 보존됨)
    accountHistoryService.log(publicId, AccountEventType.ACCOUNT_DELETE,
                              deletedBy, metadata, true, null);
    accountRepository.delete(account); // Hard delete (CASCADE로 RefreshToken도 삭제)
}
</good-example>

<bad-example>
// FORBIDDEN: 평문 비밀번호 비교
if (rawPassword.equals(user.getPassword())) { // FORBIDDEN - 매우 위험
    // 로그인 성공
}

// FORBIDDEN: 감사 이력 없이 삭제
public void deleteUser(String publicId) {
    userRepository.deleteByPublicId(publicId); // FORBIDDEN - 감사 이력 미기록
}

// FORBIDDEN: Primary key 노출
public Long getAccountId(String username) { // FORBIDDEN
    return accountRepository.findByUsername(username).getId();
}
</bad-example>

### Spring Boot Auto-Configuration 요구사항 (MANDATORY)

**validator auto-configuration 수정 시:**

- NEVER 버전 증가 없이 하위 호환성 깨기
- ALWAYS 모든 configuration property에 합리적인 기본값 제공
- MUST feature toggle에 @ConditionalOnProperty 사용
- ALWAYS META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports에 등록

**새 configuration property 추가 시:**

- ALWAYS JwtValidatorProperties에 @ConfigurationProperties와 함께 추가
- MUST 즉시 사용 가능한 기본값 제공
- ALWAYS JavaDoc과 CLAUDE.md에 property 목적 문서화
- Property 이름은 MUST 패턴 준수: hanju.jwt.validator.\*

### DTO 사용 요구사항 (MANDATORY)

**Controller Layer MUST:**

- ALWAYS Request DTO 사용 (LoginRequest, RegisterRequest, AccountCreateRequest, AccountUpdateRequest)
- ALWAYS Response DTO 반환 (AccountResponse, TokenInfo)
- NEVER Entity를 직접 반환하거나 받기

**Service Layer MUST:**

- Request DTO를 받아서 비즈니스 로직 수행
- Domain model 또는 Response DTO 반환
- NEVER Entity를 service 메서드 반환 타입으로 사용

<good-example>
// Controller
@PostMapping("/register")
public AccountResponse register(@RequestBody RegisterRequest request) {
    Account account = authService.register(request);
    return AccountResponse.from(account);
}

// Service
public Account register(RegisterRequest request) {
// DTO로 받아서 Domain model 반환
AccountEntity entity = new AccountEntity();
entity.setUsername(request.getUsername());
// ...
entity = accountRepository.save(entity);
return entity.toDomain();
}
</good-example>

<bad-example>
// FORBIDDEN: Controller에서 Entity 직접 반환
@GetMapping("/accounts/{id}")
public AccountEntity getAccount(@PathVariable Long id) { // FORBIDDEN
    return accountRepository.findById(id).orElseThrow();
}

// FORBIDDEN: Service에서 Entity 반환
public AccountEntity createAccount(AccountCreateRequest request) { // FORBIDDEN
AccountEntity entity = new AccountEntity();
// ...
return accountRepository.save(entity);
}
</bad-example>

### 코드 조직 요구사항 (MANDATORY)

**Package 구조:**

- validator: ONLY me.hanju.auth.validator.\* 패키지
  - `autoconfigure/`: Spring Boot 자동 구성 관련 클래스 (JwtValidatorAutoConfiguration, JwtValidatorConfigurer, JwtAuthenticationFilter)
  - `config/`: Configuration Properties만 (JwtValidatorProperties)
  - `domain/`: 순수 도메인 모델 (Account, TokenInfo, JwtAuthenticationToken)
  - `enums/`: Enum 타입 전용 (AccountRole, AccountStatus, JwtTokenType)
  - `exception/`: 예외 클래스 (JwtValidationException)
  - `service/`: 비즈니스 로직 서비스 (JwtTokenService)
- authenticator: ONLY me.hanju.auth.authenticator.\* 패키지
- NEVER 모듈 간 순환 의존성 생성

**명명 규칙:**

- Entity는 MUST "Entity" 접미사 (AccountEntity, RefreshTokenEntity, AccountHistoryEntity)
- DTO는 MUST "Request" 또는 "Response" 접미사
- Domain model은 MUST 접미사 없음 (Account, TokenInfo)
- Enum은 별도 enums 패키지에 위치 (AccountRole, AccountStatus, JwtTokenType)
- Service는 MUST "Service" 접미사

**우선순위 (충돌 시):**

1. 보안 요구사항 (비밀번호 처리, 토큰 검증) - HIGHEST PRIORITY
2. 타입 안전성 (domain model vs entity 분리) - HIGH PRIORITY
3. Spring Boot 모범 사례 (auto-configuration, properties)
4. 코드 조직 (package 구조, 명명)
5. 성능 최적화 - LOWEST PRIORITY

## Project Overview

This is a multi-module JWT authentication library consisting of:

- **validator**: A thin-jar library providing JWT validation capabilities for microservices
- **authenticator**: A standalone authentication server that issues JWT tokens

The validator module is designed to be published and consumed by multiple microservices, while the authenticator server manages user authentication, registration, and token lifecycle.

## Build Commands

### Build entire project

```bash
./gradlew build
```

### Build specific module

```bash
./gradlew :validator:build
./gradlew :authenticator:build
```

### Publish validator library

```bash
# To Maven Local (for development)
./gradlew :validator:publishToMavenLocal

# Check published artifact
ls ~/.m2/repository/com/github/hanju-darby/jwt-validator/1.0.0/
```

### Publish to JitPack

**Prerequisites:**
1. Push code to GitHub repository
2. Create a Git tag for the version

```bash
# Create and push a tag
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

**JitPack will automatically:**
- Build the project when someone requests the dependency
- Cache the build artifacts
- Serve the library to consumers

**Using in other projects:**

```gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.hanju-darby:hanju-auth:jwt-validator:v1.0.0'
}
```

**Alternative version formats:**
```gradle
# Using tag
implementation 'com.github.hanju-darby:hanju-auth:jwt-validator:v1.0.0'

# Using commit hash
implementation 'com.github.hanju-darby:hanju-auth:jwt-validator:abc1234'

# Using branch (snapshot)
implementation 'com.github.hanju-darby:hanju-auth:jwt-validator:main-SNAPSHOT'
```

**JitPack build status:**
Visit `https://jitpack.io/#hanju-darby/hanju-auth` to see build logs and available versions.

### Run authenticator server

```bash
# Using Gradle
./gradlew :authenticator:bootRun

# Build and run JAR
./gradlew :authenticator:bootJar
java -jar authenticator/build/libs/authenticator.jar
```

### Clean build artifacts

```bash
./gradlew clean
```

## Architecture

### Multi-Module Structure

**validator** (Java Library - Thin JAR)

- Purpose: Reusable JWT validation library for microservices
- Artifact: `me.hanju.auth:jwt-validator:1.0.0`
- Package structure (5 packages):
  - `autoconfigure/`: Spring Boot auto-configuration (JwtValidatorAutoConfiguration, JwtValidatorConfigurer, JwtAuthenticationFilter)
  - `config/`: Configuration properties (JwtValidatorProperties)
  - `domain/`: Domain models (Account, TokenInfo, JwtAuthenticationToken)
  - `enums/`: Enum types (AccountRole, AccountStatus, JwtTokenType)
  - `exception/`: Exception classes (JwtValidationException)
  - `service/`: Business logic (JwtTokenService)
- Key components:
  - `JwtTokenService`: Core JWT generation and validation logic
  - `JwtAuthenticationFilter`: Servlet filter for JWT extraction and validation
  - `JwtValidatorConfigurer`: Fluent API for Spring Security integration
  - `JwtValidatorAutoConfiguration`: Spring Boot auto-configuration
- Packaging: Thin JAR (dependencies not included) for library consumption

**authenticator** (Spring Boot Application - Fat JAR)

- Purpose: Standalone authentication server
- Depends on: validator module (internal project dependency)
- Key components:
  - `AuthenticationService`: Handles login, registration, token refresh
  - `AccountService`: Account management operations (CRUD, search, lock/unlock, status change)
  - `AccountHistoryService`: Centralized audit event logging (모든 계정 이벤트 기록)
  - Entities: `AccountEntity`, `RefreshTokenEntity`, `AccountHistoryEntity`
  - Repositories: JPA repositories for account data persistence
  - Controllers: REST endpoints for auth and account operations
  - Utilities: `NanoIdGenerator` (10-char URL-safe IDs), `HttpRequestMetadataExtractor` (IP/User-Agent)
- Packaging: Fat JAR (all dependencies included) for standalone deployment

### Dependency Flow

```
authenticator (Fat JAR)
    ↓ depends on
validator (Thin JAR)
    ↓ published to
JitPack / Maven Local
    ↓ consumed by
Other Microservices (Resource Servers)
```

### Key Architectural Patterns

**Auto-Configuration Pattern**
The validator uses Spring Boot's auto-configuration mechanism:

- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` registers `JwtValidatorAutoConfiguration`
- Configuration activates when `hanju.jwt.validator.enabled=true` (default)
- Creates beans: `JwtTokenService`, `JwtAuthenticationFilter`, `JwtValidatorConfigurer`

**Domain-Driven Separation**

- validator module:
  - Domain models (`Account`, `TokenInfo`, `JwtAuthenticationToken`) with no persistence logic
  - Enum types in separate `enums` package (`AccountRole`, `AccountStatus`, `JwtTokenType`)
  - Clear separation between domain concepts and technical infrastructure
- authenticator module: JPA entities (`AccountEntity`) that convert to/from domain models
- Pattern: `AccountEntity.toDomain()` → `Account` (for validator)

**Security Integration**

- validator provides `JwtValidatorConfigurer.configure(HttpSecurity)` for easy integration
- authenticator's `AuthenticatorSecurityConfig` uses this configurer
- Pattern: Consuming applications inject `JwtValidatorConfigurer` and call `configure(http)` in their SecurityFilterChain

**Token Lifecycle Management**

- authenticator maintains `RefreshTokenEntity` in database
- Tracks token usage, expiration, revocation
- Supports "remember me" with extended refresh token lifetime

### Configuration

**validator configuration** (`hanju.jwt.validator.*`):

- `enabled`: Enable/disable auto-configuration (default: true)
- `secret-key`: Base64-encoded HMAC secret (must be same across all services)
- `issuer`: JWT issuer claim (default: "hanju-auth")
- `access-token-expire-minutes`: Access token lifetime (default: 1440 = 24 hours)
- `refresh-token-multiplier`: Refresh token lifetime multiplier (default: 10)
- `permit-all-patterns`: URL patterns that don't require authentication
- `authenticated-patterns`: URL patterns that require authentication
- `skip-filter-patterns`: Patterns to skip JWT filter entirely

**authenticator configuration** (`hanju.authenticator.*`):

- `enabled`: Enable/disable authenticator security config (default: true)
- Uses all validator configuration plus database configuration

### Database Schema

**account** table (managed by AccountEntity):

- `account_id`: Primary key (auto-increment)
- `public_id`: Public-facing ID (10-char Nano ID, unique, URL-safe)
- `username`: Login identifier (unique, trimmed)
- `password`: BCrypt hashed password
- `role`: Enum (ROLE_ADMIN, ROLE_USER, ROLE_GUEST)
- `status`: Enum (PENDING, ACTIVE, INACTIVE) - 계정 상태 관리
- `last_login_at`: Timestamp of last successful login
- `password_error_count`: Failed login attempts (locks at 5)
- `locked`: Boolean flag for account lockout
- JPA Audit fields (automatic): `created_at`, `created_by`, `last_modified_at`, `last_modified_by`
  - AuditorAware가 SecurityContext에서 Account.publicId 추출하여 자동 기록

**refresh_token** table (managed by RefreshTokenEntity):

- Stores refresh tokens with metadata (IP, User Agent)
- Tracks `last_used_at` and `revoked_at`
- FK relationship with account (cascade on delete)
- Uses @JsonBackReference to prevent circular serialization

**account_history** table (managed by AccountHistoryEntity):

- Records all account events: LOGIN_ATTEMPT, LOGIN_SUCCESS, LOGOUT, TOKEN_REFRESH, REGISTER, ACCOUNT_CREATE, ACCOUNT_UPDATE, ACCOUNT_DELETE, ACCOUNT_STATUS_CHANGE, ACCOUNT_LOCK, ACCOUNT_UNLOCK, PASSWORD_CHANGE, PASSWORD_RESET
- Stores `account_public_id` (String) instead of FK for loose coupling
- Preserves audit trail even after account hard-delete
- Captures: IP address, User Agent, event type, performed_by, success flag, details (JSON Map)
- Centralized logging via AccountHistoryService (eliminates code duplication)
- Index on `account_public_id`, `event_type`, `created_at` for efficient queries

## Important Implementation Details

### Password Security

- Passwords are hashed using BCrypt via `PasswordEncoder`
- Account locks after 5 failed login attempts (`AccountEntity.increasePasswordErrorCount()`)
- Password changes revoke all existing refresh tokens for security

### Account Deletion with Audit Trail Preservation

현재 구현은 hard delete를 사용하지만, AccountHistory는 느슨한 결합으로 보존됩니다:

- AccountEntity 삭제 전에 AccountHistoryService로 ACCOUNT_DELETE 이벤트 기록 필수
- AccountHistoryEntity는 FK가 아닌 String accountPublicId를 사용하여 계정 삭제 후에도 감사 이력 보존
- RefreshToken은 CASCADE DELETE로 자동 삭제
- Soft delete는 향후 고려사항 (현재는 hard delete 사용)

### Nano ID Generation

Public-facing account IDs use Nano ID (10 characters) instead of exposing database primary keys:

- Generated in `AccountEntity.prePersist()`
- Provides URL-safe, collision-resistant identifiers

### JPA Auditing Configuration

The authenticator uses Spring Data JPA Auditing to automatically populate audit fields:

**Configuration** (`AuthenticatorJpaConfig`):

- `@EnableJpaAuditing(auditorAwareRef = "auditorProvider")`
- `AuditorAware<String>` bean extracts `publicId` from JWT Authentication in SecurityContext
- Falls back to "system" for anonymous/unauthenticated requests

**AccountEntity Dual Audit Strategy**:

- **JPA Auditing (automatic)**: `@CreatedDate`, `@CreatedBy`, `@LastModifiedDate`, `@LastModifiedBy`
  - Tracks ALL database-level changes (every save/update)
  - Populated automatically by JPA on every transaction
- **Business Auditing (manual)**: `updatedAt`, `updatedBy`
  - Tracks only meaningful business updates (admin modifications, profile changes)
  - Must be set manually in service layer methods
  - Distinguishes business logic updates from technical DB changes (e.g., passwordErrorCount increment)

**Usage Pattern**:

```java
// Service layer - manually set business audit fields
public AccountResponse updateAccount(String publicId, AccountUpdateRequest request, String performedBy) {
    AccountEntity account = findByPublicId(publicId);
    // ... update fields ...
    Instant now = Instant.now();
    account.setUpdatedAt(now);      // Manual business audit
    account.setUpdatedBy(performedBy);
    accountRepository.save(account); // JPA audit fields auto-populated
    return AccountResponse.from(account);
}
```

### Account History Loose Coupling

**Design Decision**: `AccountHistoryEntity` uses `String accountPublicId` instead of FK relationship to `AccountEntity`.

**Rationale**:

- Preserves audit trail even after account hard-delete
- Avoids cascade/restrict constraints complications
- Enables historical data retention for compliance
- No dependency on parent entity existence

**Implementation**:

```java
@Column(name = "account_public_id", length = 10, nullable = false)
private String accountPublicId; // No @ManyToOne, no FK constraint

@Index(name = "idx_account_public_id_event_time", columnList = "account_public_id, event_time")
```

**AccountHistoryService**:

- Centralized service for logging all account events
- Used by `AccountService` and `AuthenticationService`
- Eliminates code duplication

### Security Filter Chain Order

The validator's `JwtAuthenticationFilter` is automatically added to the Spring Security filter chain by `JwtValidatorConfigurer`. It runs before the default authentication filters.

### Token Claims Structure

Access tokens include:

- Standard claims: `sub` (publicId), `iss` (issuer), `iat`, `exp`
- Custom claims: `username`, `role`, `type` (ACCESS/REFRESH)

Refresh tokens have longer expiration but same structure.

## Common Development Workflows

### Adding a new endpoint to authenticator

1. Create DTO in `me.hanju.auth.authenticator.dto`
2. Add business logic to appropriate service
3. Create controller method in `me.hanju.auth.authenticator.controller`
4. Update `AuthenticatorSecurityConfig.authenticatorSecurityFilterChain()` if endpoint needs special auth rules

### Modifying JWT validation logic

1. Edit `validator/src/main/java/me/hanju/auth/validator/service/JwtTokenService.java`
2. Ensure backward compatibility if validators are deployed across multiple services
3. Update version in root `build.gradle` if breaking change
4. Republish validator: `./gradlew :validator:publishToMavenLocal`

### Adding a new account field

1. Add field to `AccountEntity` (with appropriate JPA annotations)
2. Add field to `Account` domain model in validator module
3. Update `AccountEntity.toDomain()` and `AccountEntity.updateFromDomain()`
4. Update DTOs: `AccountCreateRequest`, `AccountUpdateRequest`, `AccountResponse`
5. Update service layer: `AccountService`, `AuthenticationService`
6. Consider database migration strategy (liquibase/flyway not yet configured)

## Environment Variables

**Required for production**:

- `JWT_SECRET_KEY`: Base64-encoded secret (must be same across all services using validator)
- `DB_URL`: JDBC URL (e.g., `jdbc:mariadb://localhost:3306/auth_db`)
- `DB_USERNAME`: Database user
- `DB_PASSWORD`: Database password

**Optional**:

- `DB_DRIVER`: JDBC driver class (defaults to H2 for development)

## Port Configuration

- authenticator server: `8090` (configured in `authenticator/src/main/resources/application.yml`)
- H2 console (dev only): `http://localhost:8090/h2-console`
- Swagger UI: `http://localhost:8090/docs`

## Post-Refactoring Checklist

**⚠️ CRITICAL: ALWAYS run this checklist after major refactoring operations**

When you complete refactoring that involves:

- Renaming entities, repositories, services, or controllers
- Changing API endpoints or URL structures
- Modifying DTO/Entity mappings or field names
- Restructuring package hierarchy

**IMMEDIATELY** follow this verification process:

### Quick Checklist (20-30 minutes)

```bash
# 1. Compilation check (2 min)
./gradlew clean compileJava

# 2. Find JPQL parameter mismatches (HIGH PRIORITY - 3 min)
cd authenticator/src/main/java
grep -B2 -A5 "@Query" */repository/*.java | grep -E "@Param|:"
# VERIFY: All :placeholder names match @Param("name") strings

# 3. Find old entity/service references (3 min)
grep -r "AuthUserEntity\|UserManagementService" --include="*.java" .
# VERIFY: Zero matches (already refactored to Account*)
```

### Common Errors After Refactoring

**1. JPQL @Param Mismatches** (Most Common)

```java
// WRONG - Will fail at runtime
@Query("SELECT rt FROM RefreshTokenEntity rt WHERE rt.account = :authUser")
List<RefreshTokenEntity> find(@Param("account") AccountEntity account);
// Error: Named parameter not bound [authUser]

// CORRECT - Placeholder matches @Param
@Query("SELECT rt FROM RefreshTokenEntity rt WHERE rt.account = :account")
List<RefreshTokenEntity> find(@Param("account") AccountEntity account);
```

**2. Integration Test URL Mismatches**

```java
// Test uses old URL (WRONG)
restTemplate.exchange(baseUrl + "/api/admin/users", ...)  // 404 Not Found

// But controller was refactored to (CORRECT)
@RequestMapping("/api/accounts")  // New path
```

**3. Entity/Domain Model Type Confusion**

```java
// WRONG - Passing entity where domain model expected
jwtTokenService.generateToken(accountEntity);  // ClassCastException

// CORRECT - Convert entity to domain model first
Account domainAccount = accountEntity.toDomain();
jwtTokenService.generateToken(domainAccount);
```

### Detailed Documentation

See [docs/POST_REFACTORING_CHECKLIST.md](docs/POST_REFACTORING_CHECKLIST.md) for:

- Complete verification process with grep commands
- CI/CD integration examples
- Real error examples from this project
- Static analysis patterns
- Prevention strategies

**Time Investment**: 20-30 minutes of systematic checking saves hours/days of debugging production issues.

**Last Major Refactoring**: 2025-10-15 (AuthUser → Account, UserManagement → Account)
