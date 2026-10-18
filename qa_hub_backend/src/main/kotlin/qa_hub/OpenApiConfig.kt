package qa_hub

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class OpenApiConfig {
    @Bean
    open fun qaHubOpenApi(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("QA Hub API")
                    .description("HTTP API of the QA Hub backend")
                    .version("0.0.1")
            )
    }

    @Bean
    open fun allApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("all")
            .pathsToMatch("/api/**")
            .build()
    }

    @Bean
    open fun testRunsApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("test-runs")
            .pathsToMatch(
                "/api/testRuns/**",
                "/api/testResults/**",
                "/api/testRunForms/**",
                "/api/attachments/**",
                "/api/stats/**"
            )
            .build()
    }

    @Bean
    open fun integrationsApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("integrations")
            .pathsToMatch(
                "/api/cicd/**",
                "/api/tms/**",
                "/api/taskTracker/**",
                "/api/integrations/**",
                "/api/slack/**"
            )
            .build()
    }

    @Bean
    open fun adminApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("admin")
            .pathsToMatch(
                "/api/projects/**",
                "/api/platforms/**",
                "/api/config/**",
                "/api/utils/**",
                "/api/metrics/**",
                "/api/mongo/**",
                "/api/blocker/**",
                "/api/testcases/**"
            )
            .build()
    }
}
