package de.sharetopia.productservice.product

import de.sharetopia.productservice.Product
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/v1/")
class ProductController {

    @Autowired
    private lateinit var productService: ProductService

    @GetMapping("/products")
    fun getAllProducts(): List<Product?>? {
        return productService.findAll()
    }

    @PostMapping("/products")
    fun saveOrUpdateProduct(@RequestBody product: Product): Product {
        return productService.saveOrUpdateProduct(product)
    }

    @GetMapping("/products/{id}")
    fun getProductById(@PathVariable(value = "id") productId: String): ResponseEntity<Product> {
        return productService.findProductById(productId).map { prd ->
            ResponseEntity.ok(prd)
        }.orElse(ResponseEntity.notFound().build())
    }


}