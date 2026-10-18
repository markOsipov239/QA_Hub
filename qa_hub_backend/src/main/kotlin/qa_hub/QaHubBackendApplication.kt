package qa_hub

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import org.bson.types.ObjectId
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@SpringBootApplication
@EnableScheduling
open class QaHubBackendApplication

fun main(args: Array<String>) {
    runApplication<QaHubBackendApplication>(*args)
}

@Configuration
@EnableWebMvc
open class ApplicationConfig: WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .maxAge(3600)
    }

    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>?>) {
        // springdoc отдаёт /v3/api-docs* как byte[]. Если Jackson стоит раньше,
        // он сериализует массив байт в JSON-строку (base64), и Swagger UI не парсит спеку.
        converters.removeIf { it is ByteArrayHttpMessageConverter }
        converters.add(0, ByteArrayHttpMessageConverter())

        converters.filterIsInstance<MappingJackson2HttpMessageConverter>().forEach { converter ->
            converter.objectMapper = jacksonObjectMapper()
        }
    }

    //Converting mongo object id as string
    private fun jacksonObjectMapper(): ObjectMapper = Jackson2ObjectMapperBuilder()
        .serializerByType(ObjectId::class.java, ToStringSerializer())
        .build<ObjectMapper>()
}