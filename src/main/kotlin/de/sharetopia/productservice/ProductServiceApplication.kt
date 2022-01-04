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
import de.sharetopia.productservice.product.model.*
import de.sharetopia.productservice.product.repository.ElasticProductRepository
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.geo.Distance
import org.springframework.data.geo.Metrics
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import java.net.MalformedURLException
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SpringBootApplication
class ProductServiceApplication : CommandLineRunner {

  @Autowired private lateinit var productRepository: ProductRepository
  @Autowired private lateinit var elasticProductRepository: ElasticProductRepository

  @Autowired private lateinit var jwtConfiguration: JwtConfiguration

  override fun run(vararg args: String) {
    productRepository.deleteAll()
    elasticProductRepository.deleteAll()

      val savedModel = productRepository.save(
          ProductModel(
              title="Fahrrad",
              description = "Mein tolles neues Fahrrad hat Bremse, Hupe und Licht.",
              tags = listOf("Fahrrad", "Mobilität"),
              address = Address("Ludwigsburger Straße 11","Backnang", "71522"),
              //format is lng-lat
              location = listOf(9.430380,48.923069),
              rentableDateRange = DateRangeDuration(
                  LocalDate.parse("2021-05-05", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                  LocalDate.parse("2022-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
              ),
              rents = mutableListOf(
                  Rent("1234",
                      DateRangeDuration(
                          LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                          LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                      ),
                      "235423532453453"
                  )
              )
          )
      )

      val savedModel1 = productRepository.save(
          ProductModel(
              title="Auto",
              description = "Mein tolles neues Auto hat Bremse, Hupe und Licht.",
              tags = listOf("Fahrrad", "Mobilität"),
              address = Address("Ludwigsburger Straße 11","Backnang", "71522"),
              //format is lng-lat
              location = listOf(9.430380,48.923069),
              rentableDateRange = DateRangeDuration(
                  LocalDate.parse("2021-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                  LocalDate.parse("2025-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
              ),
              rents = mutableListOf(
                  Rent("214234235",
                      DateRangeDuration(
                          LocalDate.parse("2021-11-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                          LocalDate.parse("2021-12-15", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                      ),
                  "12432342353425345213"
                  ),
                  Rent("5324234",
                      DateRangeDuration(
                          LocalDate.parse("2021-10-10", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                          LocalDate.parse("2021-10-20", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                      ),
                      "234210230575"
                  )
              )
          )
      )

    elasticProductRepository.save(ObjectMapperUtils.map(savedModel, ElasticProductModel::class.java))
    elasticProductRepository.save(ObjectMapperUtils.map(savedModel1, ElasticProductModel::class.java))

    val products =  productRepository.findByLocationNear(GeoJsonPoint(9.430380, 48.923069), Distance(5.0, Metrics.KILOMETERS))

    print(products)
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
        jwtProcessor.setJWSKeySelector(keySelector)
        return jwtProcessor
    }
}

fun main(args: Array<String>) {
  runApplication<ProductServiceApplication>(*args)
}
