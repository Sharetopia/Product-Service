package de.sharetopia.productservice

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
        Product(
            "Fahrrad",
            "Mein tolles neues Fahrrad hat Bremse, Hupe und Licht.",
            listOf("Fahrrad", "Mobilität")
        )
    )

    val products = productRepository.findAll()

    print(products)
  }
}

fun main(args: Array<String>) {
  runApplication<ProductServiceApplication>(*args)
}
