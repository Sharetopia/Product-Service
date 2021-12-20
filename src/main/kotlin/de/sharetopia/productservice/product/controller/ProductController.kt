package de.sharetopia.productservice.product.controller

import de.sharetopia.productservice.product.dto.ProductDTO
import de.sharetopia.productservice.product.dto.ProductView
import de.sharetopia.productservice.product.exception.ProductNotFoundException
import de.sharetopia.productservice.product.model.ElasticProductModel
import de.sharetopia.productservice.product.model.ProductModel
import de.sharetopia.productservice.product.service.ElasticProductService
import de.sharetopia.productservice.product.service.ProductService
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/v1/")
class ProductController {

  @Autowired private lateinit var productService: ProductService
  @Autowired private lateinit var elasticProductService: ElasticProductService

  @GetMapping("/products")
  fun getAll(): List<ProductView> {
    return ObjectMapperUtils.mapAll(productService.findAll(), ProductView::class.java)
  }

  @PostMapping("/products")
  fun create(@RequestBody productDTO: ProductDTO): ProductView {
    val requestProductModel = ObjectMapperUtils.map(productDTO, ProductModel::class.java)
    val createdProductModel = productService.create(requestProductModel)
    var elasticProductModel = ObjectMapperUtils.map(createdProductModel, ElasticProductModel::class.java)
    elasticProductService.save(elasticProductModel)
    return ObjectMapperUtils.map(createdProductModel, ProductView::class.java)
  }

  @PutMapping("/products/{id}")
  fun updateOrInsert(@PathVariable(value = "id") productId: String, @RequestBody productDTO: ProductDTO): ProductView {
    val requestProductModel = ObjectMapperUtils.map(productDTO, ProductModel::class.java)
    val updatedProductModel = productService.updateOrInsert(productId, requestProductModel)
    var elasticProductModel = ObjectMapperUtils.map(updatedProductModel, ElasticProductModel::class.java)
    elasticProductService.save(elasticProductModel)

    return ObjectMapperUtils.map(updatedProductModel, ProductView::class.java)
  }

  @GetMapping("/products/{id}")
  fun getById(@PathVariable(value = "id") productId: String): ResponseEntity<ProductView> {
    return productService
        .findById(productId)
        .map { prd -> ResponseEntity.ok(ObjectMapperUtils.map(prd, ProductView::class.java)) }
        .orElseThrow { ProductNotFoundException(productId) }
  }

  @GetMapping("/products/batch/{ids}")
  fun getByMultipleIds(@PathVariable(value = "ids") productIdList: List<String>, @RequestParam(defaultValue = "0") pageNo: Int,
                       @RequestParam(defaultValue = "10") pageSize: Int): Page<ProductView> {

    val paging: Pageable = PageRequest.of(pageNo, pageSize)
    return ObjectMapperUtils.map(productService.findManyById(productIdList, paging), Page.empty<ProductView>()::class.java)
  }

  @GetMapping("/products/searchExecution")
  fun executeSearch(@RequestParam("term") searchTerm: String, @RequestParam(defaultValue = "0") pageNo: Int,
                           @RequestParam(defaultValue = "10") pageSize: Int): ResponseEntity<Any> {
    val paging: Pageable = PageRequest.of(pageNo, pageSize)
    val foundProducts = elasticProductService.findByTitle(searchTerm, paging)
    return ResponseEntity.ok(ObjectMapperUtils.map(foundProducts, Page.empty<ProductView>()::class.java))
  }

  @GetMapping("/products/findNearCoordinates")
  fun findRelevantProductsByCoordinates(@RequestParam("term") searchTerm: String, @RequestParam("distance") distance: Int, @RequestParam(defaultValue = "0") pageNo: Int,
                           @RequestParam(defaultValue = "10") pageSize: Int,@RequestParam("lat") lat: Double,@RequestParam("lon") lon: Double): ResponseEntity<Any> {
    val paging: Pageable = PageRequest.of(pageNo, pageSize)
    val foundProducts = elasticProductService.findByTitleAndNearCoordinates(searchTerm, distance, paging, lat, lon)
    return ResponseEntity.ok(ObjectMapperUtils.map(foundProducts, Page.empty<ProductView>()::class.java))
  }

  @GetMapping("/products/findNearCity")
  fun findRelevantProductsByZipOrCity(@RequestParam("term") searchTerm: String, @RequestParam("distance") distance: Int, @RequestParam(defaultValue = "0") pageNo: Int,
                           @RequestParam(defaultValue = "10") pageSize: Int,@RequestParam("cityIdentifier") cityIdentifier: String): ResponseEntity<Any> {
    val paging: Pageable = PageRequest.of(pageNo, pageSize)

    val foundProducts = elasticProductService.findByTitleAndNearCity(searchTerm, distance, paging, cityIdentifier)
    return ResponseEntity.ok(ObjectMapperUtils.map(foundProducts, Page.empty<ProductView>()::class.java))
  }

  @DeleteMapping("/products/{id}")
  fun deleteById(@PathVariable(value = "id") productId: String): ResponseEntity<Any> {
    productService.findById(productId).orElseThrow { ProductNotFoundException(productId) }
    productService.deleteById(productId)

    elasticProductService.deleteById(productId)

    return ResponseEntity<Any>(HttpStatus.OK)
  }

  @PatchMapping("/products/{id}")
  fun partialUpdate(@PathVariable(value = "id") productId: String, @RequestBody productDTO: ProductDTO): ProductView {
    val storedProductModel = productService.findById(productId).orElseThrow { ProductNotFoundException(productId) }
    val updatedProduct = productService.updateOrInsert(productId, storedProductModel.copy(
      title = productDTO.title ?: storedProductModel.title,
      description = productDTO.description ?: storedProductModel.description,
      tags = productDTO.tags ?: storedProductModel.tags
    ))

    elasticProductService.save(ObjectMapperUtils.map(updatedProduct, ElasticProductModel::class.java))
    return ObjectMapperUtils.map(updatedProduct, ProductView::class.java)
  }

}
