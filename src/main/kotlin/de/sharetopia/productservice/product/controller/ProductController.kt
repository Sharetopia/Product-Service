package de.sharetopia.productservice.product

import de.sharetopia.productservice.ProductModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/")
class ProductController {

  @Autowired private lateinit var productService: ProductService

  @GetMapping("/products")
  fun getAll(): List<ProductModel> {
    return productService.findAll()
  }

  @PostMapping("/products")
  fun saveOrUpdate(@RequestBody product: ProductModel): ProductModel {
    return productService.saveOrUpdate(product)
  }

  @GetMapping("/products/{id}")
  fun getById(@PathVariable(value = "id") productId: String): ResponseEntity<ProductModel> {
    return productService
        .findById(productId)
        .map { prd -> ResponseEntity.ok(prd) }
        .orElse(ResponseEntity.notFound().build())
  }
}
