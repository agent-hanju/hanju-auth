package me.hanju.auth.authenticator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI (Swagger) 설정
 *
 * <p>
 * JWT 인증을 Swagger UI에서 테스트할 수 있도록 Security Scheme을 설정합니다.
 */
@Configuration
public class OpenApiConfig {

  private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Authentication Server API")
                .version("1.0.0")
                .description("""
                    JWT 기반 인증 서버 API

                    ## 사용 방법
                    1. `/api/auth/register` 또는 `/api/auth/login`으로 계정 생성/로그인
                    2. 응답에서 받은 `accessToken` 복사
                    3. 우측 상단 **Authorize** 버튼 클릭
                    4. `Bearer {accessToken}` 형식으로 입력 (예: `Bearer eyJhbGc...`)
                    5. 인증이 필요한 API 테스트 가능
                    """)
                .contact(
                    new Contact()
                        .name("한주")
                        .email("agent.hanju@gmail.com")))
        .components(
            new Components()
                .addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다.")))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
  }
}
