package de.sharetopia.productservice

import com.nimbusds.jose.JWSAlgorithm.RS256
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.util.DefaultResourceRetriever
import com.nimbusds.jose.util.ResourceRetriever
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import de.sharetopia.productservice.product.auth.JwtConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.net.MalformedURLException
import java.net.URL

@SpringBootApplication
class ProductServiceApplication : CommandLineRunner {

    @Autowired
    private lateinit var jwtConfiguration: JwtConfiguration

    override fun run(vararg args: String) {

    }

    @Bean
    @Throws(MalformedURLException::class)
    fun configurableJWTProcessor(): ConfigurableJWTProcessor<*>? {
        val resourceRetriever: ResourceRetriever = DefaultResourceRetriever(
            jwtConfiguration.connectionTimeout,
            jwtConfiguration.readTimeout
        )
        val jwkSetURL = URL(jwtConfiguration.jwkUrl)
        val keySource = RemoteJWKSet<SecurityContext>(jwkSetURL, resourceRetriever)
        val jwtProcessor = DefaultJWTProcessor<SecurityContext>()
        val keySelector = JWSVerificationKeySelector(RS256, keySource)
        jwtProcessor.jwsKeySelector = keySelector
        return jwtProcessor
    }
}

fun main(args: Array<String>) {
    runApplication<ProductServiceApplication>(*args)
}
