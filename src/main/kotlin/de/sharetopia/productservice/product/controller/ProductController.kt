package de.sharetopia.productservice.product.controller

import de.sharetopia.productservice.product.dto.*
import de.sharetopia.productservice.product.exception.ProductNotFoundException
import de.sharetopia.productservice.product.exception.RentRequestNotFoundException
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
import java.util.*


@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v1/")
@Tag(name = "Products", description = "Endpoints for managing product listings")
class ProductController {

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
  fun partialUpdate(@PathVariable(value = "id") productId: String, @RequestBody updatedFieldsProductDTO: ProductDTO): ProductView {
    val storedProductModel = productService.findById(productId).orElseThrow { ProductNotFoundException(productId) }
    val updatedProduct = productService.partialUpdate(productId, storedProductModel, updatedFieldsProductDTO)

    elasticProductService.save(ObjectMapperUtils.map(updatedProduct, ElasticProductModel::class.java))
    return ObjectMapperUtils.map(updatedProduct, ProductView::class.java)
  }

  @Operation(summary = "Accept/reject rent request for product", description = "Endpoint to accept or reject a certain rent request (rr) for a given product. If accepted the according rent is added to product and rent request status is set accordingly")
  @PostMapping("/products/{id}/rent/{rentRequestId}")
  fun acceptOrRejectRentRequest(@PathVariable(value = "id") productId: String, @PathVariable(value = "rentRequestId") rentRequestId: String, @RequestParam("isAccepted") isAccepted: Boolean): RentRequestView {
    var rentRequest = rentRequestService.findById(rentRequestId).orElseThrow { RentRequestNotFoundException(rentRequestId) }
    var product = productService.findById(productId).orElseThrow { ProductNotFoundException(productId) }

    if(rentRequest.requestedProductId!=productId){
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "Product id specified in URL does not match product id in rent request")
    }

    if(isAccepted) {
      var updatedModel = productService.addRentToProduct(product, rentRequest)
      var elasticProductModel = ObjectMapperUtils.map(updatedModel, ElasticProductModel::class.java)
      elasticProductService.save(elasticProductModel)
    }
    var updatedRentRequest = rentRequestService.updateStatus(newStatus = if(isAccepted) "accepted" else "rejected", rentRequest)
    return ObjectMapperUtils.map(updatedRentRequest, RentRequestView::class.java)
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
  fun deleteByRentRequestId(@PathVariable(value = "id") rentRequestId: String): ResponseEntity<Any> {
    rentRequestService.findById(rentRequestId).orElseThrow { ProductNotFoundException(rentRequestId) }
    rentRequestService.deleteById(rentRequestId)

    return ResponseEntity<Any>(HttpStatus.OK)
  }

  @Operation(summary = "Create user object for current authorized user")
  @PostMapping("/user")
  fun createUser(@RequestBody userDTO: UserDTO): ResponseEntity<UserModel> {
    var user = ObjectMapperUtils.map(userDTO, UserModel::class.java)
    var createdUser = userService.save(user)
    return ResponseEntity.ok(createdUser)
  }

  @Operation(summary = "Gets user information about currently authorized user")
  @GetMapping("/user")
  fun getCurrentAuthorizedUser(principal: Principal): ResponseEntity<Optional<UserModel>> {
    var currentUserId = principal.name
    var user = userService.findById(currentUserId)
    return ResponseEntity.ok(user)
  }

  @Operation(summary = "Gets user by id")
  @GetMapping("/user/{userId}")
  fun getUser(@PathVariable(value = "id") userId: String): ResponseEntity<Optional<UserModel>> {
    var currentUserId = userId
    var user = userService.findById(currentUserId)
    return ResponseEntity.ok(user)
  }

  @Operation(summary = "Gets offered products and corresponding rent requests for authenticated user")
  @GetMapping("/user/offeredProductsOverview")
  fun getOfferedProductsOfUser(principal: Principal): ResponseEntity<MutableList<UserProductsWithRentRequestsView>> {
    var currentUserId = principal.name
    var productsAndRentRequestsForUser = productService.getProductsWithRentRequestsForUser(currentUserId)
    return ResponseEntity.ok(productsAndRentRequestsForUser)
  }

  @Operation(summary = "Gets rent requests with corresponding products for authenticated user")
  @GetMapping("/user/requestedProductsOverview")
  fun getRentRequestsOfUser(principal: Principal): ResponseEntity<MutableList<UserSentRentRequestsWithProductsView>> {
    var currentUserId = principal.name
    var rentRequestsWithProducts = productService.getRentRequestsWithProducts(currentUserId)
    return ResponseEntity.ok(rentRequestsWithProducts)
  }






}
