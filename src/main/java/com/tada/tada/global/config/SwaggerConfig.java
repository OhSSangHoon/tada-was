package com.tada.tada.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * springdoc-openapi가 자동으로 만들어주는 Swagger 문서에
 * 제목/설명 같은 기본 정보와, "인증 필요한 API 테스트용 토큰 입력창"을 추가하는 설정.
 *
 * 이 설정이 없어도 Swagger 자체는 자동으로 뜨지만(각 컨트롤러 스캔해서),
 * 아래 SecurityScheme 설정이 없으면 Swagger UI에서
 * "인증이 필요한 API"를 테스트할 때 토큰을 넣을 자리가 안 보인다.
 */
@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		// Swagger UI 우측 상단에 "Authorize" 버튼이 생기고,
		// 여기에 Access Token을 넣으면 이후 모든 API 요청에 자동으로 헤더가 붙는다.
		SecurityScheme bearerAuth = new SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT");

		return new OpenAPI()
				.info(new Info()
						.title("tada API")
						.description("타다(tada) 일기 기반 스티커 생성 서비스 API 명세")
						.version("v0.0.1"))
				.components(new Components().addSecuritySchemes("bearerAuth", bearerAuth))
				.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
	}
}