package de.sharetopia.productservice

import de.sharetopia.productservice.product.model.ProductModel
import de.sharetopia.productservice.product.repository.ProductRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ProductServiceApplication : CommandLineRunner {
  @Autowired private lateinit var productRepository: ProductRepository

  public override fun run(vararg args: String) {
    productRepository.deleteAll()

    productRepository.save(
        ProductModel(
            title="Fahrrad",
            description = "Mein tolles neues Fahrrad hat Bremse, Hupe und Licht.",
            tags = listOf("Fahrrad", "Mobilität")
        )
    )

    val products = productRepository.findAll()

    print(products)
  }
}

fun main(args: Array<String>) {
  runApplication<ProductServiceApplication>(*args)
}
