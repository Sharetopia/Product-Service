package de.sharetopia.productservice.product.controller

import de.sharetopia.productservice.product.dto.ProductDTO
import de.sharetopia.productservice.product.dto.ProductView
import de.sharetopia.productservice.product.exception.ProductNotFoundException
import de.sharetopia.productservice.product.model.ProductModel
import de.sharetopia.productservice.product.service.ProductService
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.aggregation.MergeOperation.UniqueMergeId.id
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/v1/")
class ProductController {

  @Autowired private lateinit var productService: ProductService

  @GetMapping("/products")
  fun getAll(): List<ProductView> {
    return ObjectMapperUtils.mapAll(productService.findAll(), ProductView::class.java)
  }

  @PostMapping("/products")
  fun create(@RequestBody productDTO: ProductDTO): ProductView {
    val requestProductModel = ObjectMapperUtils.map(productDTO, ProductModel::class.java)
    return ObjectMapperUtils.map(productService.create(requestProductModel), ProductView::class.java)
  }

  @PutMapping("/products/{id}")
  fun updateOrInsert(@PathVariable(value = "id") productId: String, @RequestBody productDTO: ProductDTO): ProductView {
    val requestProductModel = ObjectMapperUtils.map(productDTO, ProductModel::class.java)
    return ObjectMapperUtils.map(productService.updateOrInsert(productId, requestProductModel), ProductView::class.java)
  }

  @GetMapping("/products/{id}")
  fun getById(@PathVariable(value = "id") productId: String): ResponseEntity<ProductView> {
    return productService
        .findById(productId)
        .map { prd -> ResponseEntity.ok(ObjectMapperUtils.map(prd, ProductView::class.java)) }
        .orElseThrow { ProductNotFoundException(productId) }
  }

  @DeleteMapping("/products/{id}")
  fun deleteById(@PathVariable(value = "id") productId: String): ResponseEntity<Any> {
    productService.findById(productId).orElseThrow { ProductNotFoundException(productId) }
    productService.deleteById(productId)
    return ResponseEntity<Any>(HttpStatus.OK)
  }

  @PatchMapping("/products/{id}")
  fun partialUpdate(@PathVariable(value = "id") productId: String, @RequestBody productDTO: ProductDTO): ProductView {
    val storedProductModel = productService.findById(productId).orElseThrow { ProductNotFoundException(productId) }
    val updatedProduct = productService.updateOrInsert(productId, storedProductModel.copy( // Assuming your class is immutable
      title = productDTO.title ?: storedProductModel.title,
      description = productDTO.description ?: storedProductModel.description,
      tags = productDTO.tags ?: storedProductModel.tags
    ))
    return ObjectMapperUtils.map(updatedProduct, ProductView::class.java)
  }

}
