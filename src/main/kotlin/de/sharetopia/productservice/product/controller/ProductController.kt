package de.sharetopia.productservice.product.controller

import de.sharetopia.productservice.product.dto.*
import de.sharetopia.productservice.product.exception.NotAllowedAccessToResource
import de.sharetopia.productservice.product.exception.ProductNotFoundException
import de.sharetopia.productservice.product.model.*
import de.sharetopia.productservice.product.service.ElasticProductService
import de.sharetopia.productservice.product.service.ProductService
import de.sharetopia.productservice.product.service.RentRequestService
import de.sharetopia.productservice.product.service.UserService
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.security.Principal
import java.time.LocalDate


@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v1/")
@Tag(name = "Products", description = "Endpoints for managing product listings")
class ProductController {

  private val log: Logger = LoggerFactory.getLogger(ProductController::class.java)


  @Autowired private lateinit var productService: ProductService
  @Autowired private lateinit var elasticProductService: ElasticProductService
  @Autowired private lateinit var rentRequestService: RentRequestService
  @Autowired private lateinit var userService: UserService

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
  fun getAll(principal: Principal): List<ProductView> {
    log.info("Created product. [method=GET, endpoint=/products, requesterUserId=${principal.name}]")
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
  fun create(@RequestBody productDTO: ProductDTO, principal: Principal): ResponseEntity<ProductView> {
    val authenticatedUserId = principal.name
    val requestProductModel = ObjectMapperUtils.map(productDTO, ProductModel::class.java)
    val createdProductModel = productService.create(requestProductModel, authenticatedUserId)
    log.info("Created product. [method=POST, endpoint=/products, productId=${createdProductModel.id}, requesterUserId=${principal.name}]")
    return ResponseEntity.ok(ObjectMapperUtils.map(createdProductModel, ProductView::class.java))
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
  fun updateOrInsert(@PathVariable(value = "id") productId: String,
                     @RequestBody productDTO: ProductDTO,
                     principal: Principal): ResponseEntity<ProductView> {
    val authenticatedUserId = principal.name
    val requestProductModel = ObjectMapperUtils.map(productDTO, ProductModel::class.java)
    val updatedProductModel = productService.updateOrInsert(productId, requestProductModel, authenticatedUserId)
    log.info("Updated/inserted product. [method=PUT, endpoint=/products, productId=${updatedProductModel.id}, requesterUserId=${principal.name}]")
    return ResponseEntity.ok(ObjectMapperUtils.map(updatedProductModel, ProductView::class.java))
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
  fun getById(@PathVariable(value = "id") productId: String, principal: Principal): ResponseEntity<ProductView> {
    val requestedProduct = productService.findById(productId).orElseThrow { ProductNotFoundException(productId) }
    log.info("Updated/inserted product. [method=PUT, endpoint=/products, requesterUserId=${principal.name}]")
    return ResponseEntity.ok(ObjectMapperUtils.map(requestedProduct, ProductView::class.java))
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
                       @RequestParam(defaultValue = "10") pageSize: Int, principal: Principal): Page<ProductView> {
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
    val foundProducts = elasticProductService.findByTitleAndNearCoordinates(searchTerm, distance, lat, lon, paging)
    return ObjectMapperUtils.mapEntityPageIntoDtoPage(foundProducts, ProductView::class.java)
  }

  @Operation(summary = "Cityname/postal code based Search for available & nearby products by the title/tags", description = "Returns the products within the specified distance (in km) of the provided city name or postal code whose titles match the search term (and which are available for the optionally specified start & end date)")
  @GetMapping("/products/findNearCity")
  fun findRelevantProductsByZipOrCity(@RequestParam("term") searchTerm: String,
                                      @RequestParam("distance") distance: Int,
                                      @RequestParam(defaultValue = "0") pageNo: Int,
                                      @RequestParam(defaultValue = "10") pageSize: Int,
                                      @RequestParam("cityIdentifier") cityIdentifier: String,
                                      @RequestParam(name="startDate", required=false) @DateTimeFormat(pattern="yyyy-MM-dd") startDate: LocalDate?,
                                      @RequestParam(name="endDate", required=false) @DateTimeFormat(pattern="yyyy-MM-dd") endDate: LocalDate?): Page<ProductView> {

    val paging: Pageable = PageRequest.of(pageNo, pageSize)
    if ((startDate!=null && endDate==null) || (startDate==null && endDate!=null)) {
      throw ResponseStatusException(
        HttpStatus.BAD_REQUEST
      )
    } else if(startDate!=null && endDate!=null){
      val foundProducts = elasticProductService.findByTitleAndNearCityWithDate(searchTerm, distance, cityIdentifier,startDate,endDate, paging)
      return ObjectMapperUtils.mapEntityPageIntoDtoPage(foundProducts, ProductView::class.java)
    }
    else{
      val foundProducts = elasticProductService.findByTitleAndNearCity(searchTerm, distance, cityIdentifier, paging)
      return ObjectMapperUtils.mapEntityPageIntoDtoPage(foundProducts, ProductView::class.java)
    }

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
  fun deleteById(@PathVariable(value = "id") productId: String, principal: Principal): ResponseEntity<Any> {
    val productToBeDeleted = productService.findById(productId).orElseThrow { ProductNotFoundException(productId) }
    if(productToBeDeleted.ownerOfProductUserId!=principal.name){
      throw NotAllowedAccessToResource(principal.name)
    }
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
  fun partialUpdate(@PathVariable(value = "id") productId: String, @RequestBody updatedFieldsProductDTO: ProductDTO, principal: Principal): ProductView {
    val storedProductModel = productService.findById(productId).orElseThrow { ProductNotFoundException(productId) }

    if(storedProductModel.ownerOfProductUserId!=principal.name){
      throw NotAllowedAccessToResource(principal.name)
    }

    val updatedProduct = productService.partialUpdate(productId, storedProductModel, updatedFieldsProductDTO)

    elasticProductService.save(ObjectMapperUtils.map(updatedProduct, ElasticProductModel::class.java))
    return ObjectMapperUtils.map(updatedProduct, ProductView::class.java)
  }

  @Operation(summary = "Accept/reject rent request for product", description = "Endpoint to accept or reject a certain rent request (rr) for a given product. If accepted the according rent is added to product and rent request status is set accordingly")
  @PostMapping("/products/{id}/rent/{rentRequestId}")
  fun acceptOrRejectRentRequest(@PathVariable(value = "id") productId: String, @PathVariable(value = "rentRequestId") rentRequestId: String, @RequestParam("isAccepted") isAccepted: Boolean, principal: Principal): RentRequestView {
    return ObjectMapperUtils.map(
      productService.acceptOrRejectRentRequest(productId, rentRequestId, isAccepted, principal.name),
      RentRequestView::class.java
    )
  }

  @Operation(summary = "Create rent request")
  @PostMapping("/rentRequest")
  fun addRentRequest(@RequestBody rentRequestDTO: RentRequestDTO, principal: Principal): RentRequestView {
    val currentUserId = principal.name
    val createdRentRequestModel = rentRequestService.create(ObjectMapperUtils.map(rentRequestDTO, RentRequestModel::class.java), currentUserId)
    return ObjectMapperUtils.map(createdRentRequestModel, RentRequestView::class.java)
  }

  @Operation(summary = "Delete certain rent request")
  @DeleteMapping("/rentRequest/{id}")
  fun deleteByRentRequestId(@PathVariable(value = "id") rentRequestId: String, principal: Principal): ResponseEntity<Any> {
    val rentRequestToBeDeleted = rentRequestService.findById(rentRequestId).orElseThrow { ProductNotFoundException(rentRequestId) }
    if(rentRequestToBeDeleted.requesterUserId!=principal.name){
      throw NotAllowedAccessToResource(principal.name)
    }
    rentRequestService.deleteById(rentRequestId)

    return ResponseEntity<Any>(HttpStatus.OK)
  }
}
