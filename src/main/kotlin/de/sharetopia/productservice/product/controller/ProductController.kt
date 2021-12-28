package de.sharetopia.productservice.product.controller

import de.sharetopia.productservice.product.dto.ProductDTO
import de.sharetopia.productservice.product.dto.ProductView
import de.sharetopia.productservice.product.exception.ProductNotFoundException
import de.sharetopia.productservice.product.model.ElasticProductModel
import de.sharetopia.productservice.product.model.ProductModel
import de.sharetopia.productservice.product.service.ElasticProductService
import de.sharetopia.productservice.product.service.ProductService
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/v1/")
@Tag(name = "Products", description = "Endpoints for managing product listings")
class ProductController {

  @Autowired private lateinit var productService: ProductService
  @Autowired private lateinit var elasticProductService: ElasticProductService

  @Operation(summary = "Get all products", description = "Get all currently stored products")
  @ApiResponses(value = [
    ApiResponse(
      responseCode = "200",
      description = "Fetched products",
      content = [
        (Content(mediaType = "application/json", array = (ArraySchema(schema = Schema(implementation = ProductView::class)))))]
    )
  ])
  @GetMapping("/products")
  fun getAll(): List<ProductView> {
    return ObjectMapperUtils.mapAll(productService.findAll(), ProductView::class.java)
  }

  @Operation(summary = "Create a new product", description = "")
  @ApiResponses(value = [
    ApiResponse(
      responseCode = "200",
      description = "Created new product",
      content = [
        (Content(mediaType = "application/json", schema = Schema(implementation = ProductView::class)))]
    )
  ])
  @PostMapping("/products")
  fun create(@RequestBody productDTO: ProductDTO): ProductView {
    val requestProductModel = ObjectMapperUtils.map(productDTO, ProductModel::class.java)
    val createdProductModel = productService.create(requestProductModel)
    var elasticProductModel = ObjectMapperUtils.map(createdProductModel, ElasticProductModel::class.java)
    elasticProductService.save(elasticProductModel)
    return ObjectMapperUtils.map(createdProductModel, ProductView::class.java)
  }

  @Operation(summary = "Update or insert product", description = "Updates/inserts product depending on if the given id already exists")
  @ApiResponses(value = [
    ApiResponse(
      responseCode = "200",
      description = "Created new product",
      content = [
        (Content(mediaType = "application/json", schema = Schema(implementation = ProductView::class)))]
    )
  ])
  @PutMapping("/products/{id}")
  fun updateOrInsert(@PathVariable(value = "id") productId: String, @RequestBody productDTO: ProductDTO): ProductView {
    val requestProductModel = ObjectMapperUtils.map(productDTO, ProductModel::class.java)
    val updatedProductModel = productService.updateOrInsert(productId, requestProductModel)
    var elasticProductModel = ObjectMapperUtils.map(updatedProductModel, ElasticProductModel::class.java)
    elasticProductService.save(elasticProductModel)

    return ObjectMapperUtils.map(updatedProductModel, ProductView::class.java)
  }

  @Operation(summary = "Find product by id", description = "Returns a single product")
  @ApiResponses(value = [
    ApiResponse(
      responseCode = "200",
      description = "Success",
      content = [
        (Content(mediaType = "application/json", schema = Schema(implementation = ProductView::class)))]
    ),
    ApiResponse(
      responseCode = "404",
      description = "Product not found"
    )
  ])
  @GetMapping("/products/{id}")
  fun getById(@PathVariable(value = "id") productId: String): ResponseEntity<ProductView> {
    return productService
        .findById(productId)
        .map { prd -> ResponseEntity.ok(ObjectMapperUtils.map(prd, ProductView::class.java)) }
        .orElseThrow { ProductNotFoundException(productId) }
  }

  @Operation(summary = "Find multiple products by id", description = "Returns products for the given ids")
  @ApiResponses(value = [
    ApiResponse(
      responseCode = "200",
      description = "Success",
      content = [
        (Content(mediaType = "application/json", schema = Schema(implementation = ProductView::class)))]
    )
  ])
  @GetMapping("/products/batch/{ids}")
  fun getByMultipleIds(@PathVariable(value = "ids") productIdList: List<String>, @RequestParam(defaultValue = "0") pageNo: Int,
                       @RequestParam(defaultValue = "10") pageSize: Int): Page<ProductView> {

    val paging: Pageable = PageRequest.of(pageNo, pageSize)
    var foundProducts = productService.findManyById(productIdList, paging)
    return ObjectMapperUtils.mapEntityPageIntoDtoPage(foundProducts, ProductView::class.java)
  }

  @Operation(summary = "Search for products by the title", description = "Returns products which fit the search term")
  @GetMapping("/products/searchExecution")
  fun executeSearch(@RequestParam("term") searchTerm: String, @RequestParam(defaultValue = "0") pageNo: Int,
                           @RequestParam(defaultValue = "10") pageSize: Int): Page<ProductView> {
    val paging: Pageable = PageRequest.of(pageNo, pageSize)
    val foundProducts = elasticProductService.findByTitle(searchTerm, paging)
    return ObjectMapperUtils.mapEntityPageIntoDtoPage(foundProducts, ProductView::class.java)
  }

  @Operation(summary = "Coordinate based Search for nearby products by the title", description = "Returns the products within the specified distance (in km) of the provided coordinates whose titles match the search term")
  @GetMapping("/products/findNearCoordinates")
  fun findRelevantProductsByCoordinates(@RequestParam("term") searchTerm: String, @RequestParam("distance") distance: Int, @RequestParam(defaultValue = "0") pageNo: Int,
                           @RequestParam(defaultValue = "10") pageSize: Int,@RequestParam("lat") lat: Double,@RequestParam("lon") lon: Double): Page<ProductView> {
    val paging: Pageable = PageRequest.of(pageNo, pageSize)
    val foundProducts = elasticProductService.findByTitleAndNearCoordinates(searchTerm, distance, paging, lat, lon)
    return ObjectMapperUtils.mapEntityPageIntoDtoPage(foundProducts, ProductView::class.java)
  }

  @Operation(summary = "Cityname/postal code based Search for nearby products by the title", description = "Returns the products within the specified distance (in km) of the provided city name or postal code whose titles match the search term")
  @GetMapping("/products/findNearCity")
  fun findRelevantProductsByZipOrCity(@RequestParam("term") searchTerm: String, @RequestParam("distance") distance: Int, @RequestParam(defaultValue = "0") pageNo: Int,
                           @RequestParam(defaultValue = "10") pageSize: Int,@RequestParam("cityIdentifier") cityIdentifier: String): Page<ProductView> {
    val paging: Pageable = PageRequest.of(pageNo, pageSize)
    val foundProducts = elasticProductService.findByTitleAndNearCity(searchTerm, distance, paging, cityIdentifier)
    return ObjectMapperUtils.mapEntityPageIntoDtoPage(foundProducts, ProductView::class.java)
  }

  @Operation(summary = "Delete product by id", description = "")
  @ApiResponses(value = [
    ApiResponse(
      responseCode = "200",
      description = "Success"
    ),
    ApiResponse(
      responseCode = "404",
      description = "Product does not exist"
    )
  ])
  @DeleteMapping("/products/{id}")
  fun deleteById(@PathVariable(value = "id") productId: String): ResponseEntity<Any> {
    productService.findById(productId).orElseThrow { ProductNotFoundException(productId) }
    productService.deleteById(productId)

    elasticProductService.deleteById(productId)

    return ResponseEntity<Any>(HttpStatus.OK)
  }

  @Operation(summary = "Updates product by id", description = "Updates the provided fields of the product which belongs to the given id.")
  @ApiResponses(value = [
    ApiResponse(
      responseCode = "200",
      description = "Success",
      content = [
        (Content(mediaType = "application/json", schema = Schema(implementation = ProductView::class)))]
    ),
    ApiResponse(
      responseCode = "404",
      description = "Product not found"
    )
  ])
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
