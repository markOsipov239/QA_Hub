package qa_hub

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.module.kotlin.kotlinModule
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

    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>?>) {
        // ByteArray must be before Jackson: springdoc serves /v3/api-docs* as byte[],
        // and Jackson would otherwise base64-encode the spec.
        converters.add(ByteArrayHttpMessageConverter())
        converters.add(getJacksonConverter())
    }

    //Converting mongo object id as string
    private fun getJacksonConverter(): MappingJackson2HttpMessageConverter {
        val builder = Jackson2ObjectMapperBuilder()
            .modulesToInstall(kotlinModule())
            .serializerByType(ObjectId::class.java, ToStringSerializer())

        return MappingJackson2HttpMessageConverter(builder.build())
    }
}
