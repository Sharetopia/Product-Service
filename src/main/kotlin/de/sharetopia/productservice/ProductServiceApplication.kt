package de.sharetopia.productservice

import de.sharetopia.productservice.product.model.Address
import de.sharetopia.productservice.product.model.ElasticProductModel
import de.sharetopia.productservice.product.model.ProductModel
import de.sharetopia.productservice.product.repository.ElasticProductRepository
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.geo.Distance
import org.springframework.data.geo.Metrics
import org.springframework.data.mongodb.core.geo.GeoJsonPoint

@SpringBootApplication
class ProductServiceApplication : CommandLineRunner {
  @Autowired private lateinit var productRepository: ProductRepository
  @Autowired private lateinit var elasticProductRepository: ElasticProductRepository

  public override fun run(vararg args: String) {
    productRepository.deleteAll()
    elasticProductRepository.deleteAll()

    val savedModel = productRepository.save(
        ProductModel(
            title="Fahrrad",
            description = "Mein tolles neues Fahrrad hat Bremse, Hupe und Licht.",
            tags = listOf("Fahrrad", "Mobilität"),
            address = Address("Ludwigsburger Straße 11","Backnang", "71522"),
            //format is lng-lat
            location = listOf(9.430380,48.923069)
        )
    )
    elasticProductRepository.save(ObjectMapperUtils.map(savedModel, ElasticProductModel::class.java))

    val products =  productRepository.findByLocationNear(GeoJsonPoint(9.430380, 48.923069), Distance(5.0, Metrics.KILOMETERS))

    //val products = productRepository.findAll()

    print(products)
  }
}

fun main(args: Array<String>) {
  runApplication<ProductServiceApplication>(*args)
}
