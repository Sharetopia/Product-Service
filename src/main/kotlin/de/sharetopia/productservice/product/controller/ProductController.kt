package de.sharetopia.productservice.product.controller

import de.sharetopia.productservice.product.dto.ProductDTO
import de.sharetopia.productservice.product.dto.ProductView
import de.sharetopia.productservice.product.dto.RentRequestDTO
import de.sharetopia.productservice.product.dto.RentRequestView
import de.sharetopia.productservice.product.model.ProductModel
import de.sharetopia.productservice.product.model.RentRequestModel
import de.sharetopia.productservice.product.service.ElasticProductService
import de.sharetopia.productservice.product.service.ProductService
import de.sharetopia.productservice.product.service.RentRequestService
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
import java.security.Principal
import java.time.LocalDate


@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("/api/v1/")
@Tag(name = "Products", description = "Endpoints for managing product listings")
class ProductController {

    private val log: Logger = LoggerFactory.getLogger(ProductController::class.java)


    @Autowired
    private lateinit var productService: ProductService

    @Autowired
    private lateinit var elasticProductService: ElasticProductService

    @Autowired
    private lateinit var rentRequestService: RentRequestService

    @Operation(summary = "Get all products", description = "Get all currently stored products")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Fetched products",
                content = [
                    (Content(
                        mediaType = "application/json",
                        array = (ArraySchema(schema = Schema(implementation = ProductView::class)))
                    ))]
            )
        ]
    )
    @GetMapping("/products")
    fun getAllProducts(principal: Principal): List<ProductView> {
        log.info("Fetching all products. {method=GET, endpoint=/products, requesterUserId=${principal.name}}")
        return ObjectMapperUtils.mapAll(productService.findAll(), ProductView::class.java)
    }

    @Operation(summary = "Create a new product", description = "")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Created new product",
                content = [
                    (Content(mediaType = "application/json", schema = Schema(implementation = ProductView::class)))]
            )
        ]
    )
    @PostMapping("/products")
    fun createProduct(@RequestBody productDTO: ProductDTO, principal: Principal): ResponseEntity<ProductView> {
        log.info("Creating product. {method=POST, endpoint=/products, requesterUserId=${principal.name}}")
        val authenticatedUserId = principal.name
        val requestProductModel = ObjectMapperUtils.map(productDTO, ProductModel::class.java)
        val createdProductModel = productService.create(requestProductModel, authenticatedUserId)
        return ResponseEntity.ok(ObjectMapperUtils.map(createdProductModel, ProductView::class.java))
    }

    @Operation(
        summary = "Update or insert product",
        description = "Updates/inserts product depending on if the given id already exists"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Created new product",
                content = [
                    (Content(mediaType = "application/json", schema = Schema(implementation = ProductView::class)))]
            )
        ]
    )
    @PutMapping("/products/{id}")
    fun updateOrInsertProduct(
        @PathVariable(value = "id") productId: String,
        @RequestBody productDTO: ProductDTO,
        principal: Principal
    ): ResponseEntity<ProductView> {
        log.info("Updating/inserting product. {method=PUT, endpoint=/products, productId=${productId}, requesterUserId=${principal.name}}")
        val authenticatedUserId = principal.name
        val requestProductModel = ObjectMapperUtils.map(productDTO, ProductModel::class.java)
        val updatedProductModel = productService.updateOrInsert(productId, requestProductModel, authenticatedUserId)
        return ResponseEntity.ok(ObjectMapperUtils.map(updatedProductModel, ProductView::class.java))
    }

    @Operation(summary = "Find product by id", description = "Returns a single product")
    @ApiResponses(
        value = [
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
        ]
    )
    @GetMapping("/products/{id}")
    fun getProductById(
        @PathVariable(value = "id") productId: String,
        principal: Principal
    ): ResponseEntity<ProductView> {
        log.info("Fetching product by id. {method=GET, endpoint=/products/{id}, productId=$productId, requesterUserId=${principal.name}}")
        val requestedProduct = productService.findById(productId)
        return ResponseEntity.ok(ObjectMapperUtils.map(requestedProduct, ProductView::class.java))
    }

    @Operation(summary = "Find multiple products by id", description = "Returns products for the given ids")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Success",
                content = [
                    (Content(mediaType = "application/json", schema = Schema(implementation = ProductView::class)))]
            )
        ]
    )
    @GetMapping("/products/batch/{ids}")
    fun getProductsByMultipleIds(
        @PathVariable(value = "ids") productIdList: List<String>, @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "10") pageSize: Int, principal: Principal
    ): Page<ProductView> {
        log.info("Fetching products by ids. {method=GET, endpoint=/products/batch/{ids}, productIds=$productIdList, requesterUserId=${principal.name}}")
        val paging: Pageable = PageRequest.of(pageNo, pageSize)
        var foundProducts = productService.findManyById(productIdList, paging)
        return ObjectMapperUtils.mapEntityPageIntoDtoPage(foundProducts, ProductView::class.java)
    }

    @Operation(summary = "Search for products by the title", description = "Returns products which fit the search term")
    @GetMapping("/products/searchExecution")
    fun executeSearch(
        @RequestParam("term") searchTerm: String, @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "10") pageSize: Int, principal: Principal
    ): Page<ProductView> {
        log.info("Searching for products by search term only. {method=GET, endpoint=/products/searchExecution, searchTerm=$searchTerm, requesterUserId=${principal.name}}")
        val paging: Pageable = PageRequest.of(pageNo, pageSize)
        val foundProducts = elasticProductService.findByTitle(searchTerm, paging)
        return ObjectMapperUtils.mapEntityPageIntoDtoPage(foundProducts, ProductView::class.java)
    }

    @Operation(
        summary = "Coordinate based Search for nearby products by the title",
        description = "Returns the products within the specified distance (in km) of the provided coordinates whose titles match the search term"
    )
    @GetMapping("/products/findNearCoordinates")
    fun findRelevantProductsByCoordinates(
        @RequestParam("term") searchTerm: String,
        @RequestParam("distance") distance: Int,
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        @RequestParam("lat") lat: Double,
        @RequestParam("lon") lon: Double,
        principal: Principal
    ): Page<ProductView> {
        log.info("Searching for products by search term, coordinates and distance. {method=GET, endpoint=/products/findNearCoordinates, searchTerm=$searchTerm, lat=$lat, lon=$lon,distance=$distance, requesterUserId=${principal.name}}")
        val paging: Pageable = PageRequest.of(pageNo, pageSize)
        val foundProducts = elasticProductService.findByTitleAndNearCoordinates(searchTerm, distance, lat, lon, paging)
        return ObjectMapperUtils.mapEntityPageIntoDtoPage(foundProducts, ProductView::class.java)
    }

    @Operation(
        summary = "Cityname/postal code based Search for available & nearby products by the title/tags",
        description = "Returns the products within the specified distance (in km) of the provided city name or postal code whose titles match the search term (and which are available for the optionally specified start & end date)"
    )
    @GetMapping("/products/findNearCity")
    fun findRelevantProductsByZipOrCity(
        @RequestParam("term") searchTerm: String,
        @RequestParam("distance") distance: Int,
        @RequestParam(defaultValue = "0") pageNo: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        @RequestParam("cityIdentifier") cityIdentifier: String,
        @RequestParam(
            name = "startDate",
            required = false
        ) @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate?,
        @RequestParam(name = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate?,
        principal: Principal
    ): Page<ProductView> {
        val paging: Pageable = PageRequest.of(pageNo, pageSize)
        val foundProducts = elasticProductService.findByTitleAndNearCityWithOptionalDate(
            searchTerm,
            distance,
            cityIdentifier,
            startDate,
            endDate,
            paging
        )
        return ObjectMapperUtils.mapEntityPageIntoDtoPage(foundProducts, ProductView::class.java)
    }


    @Operation(summary = "Delete product by id", description = "")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Success"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Product does not exist"
            )
        ]
    )
    @DeleteMapping("/products/{id}")
    fun deleteProductById(@PathVariable(value = "id") productId: String, principal: Principal): ResponseEntity<Any> {
        log.info("Deleting product by id. {method=DELETE, endpoint=/products/{id}, productId=$productId, requesterUserId=${principal.name}}")
        productService.deleteById(productId, principal.name)
        return ResponseEntity<Any>(HttpStatus.OK)
    }

    @Operation(
        summary = "Updates product by id",
        description = "Updates the provided fields of the product which belongs to the given id."
    )
    @ApiResponses(
        value = [
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
        ]
    )
    @PatchMapping("/products/{id}")
    fun partialUpdate(
        @PathVariable(value = "id") productId: String,
        @RequestBody updatedFieldsProductDTO: ProductDTO,
        principal: Principal
    ): ProductView {
        log.info("Partial update to product by id. {method=PATCH, endpoint=/products/{id}, productId=$productId, requesterUserId=${principal.name}}")
        val updatedProduct = productService.partialUpdate(productId, updatedFieldsProductDTO, principal.name)
        return ObjectMapperUtils.map(updatedProduct, ProductView::class.java)
    }

    @Operation(
        summary = "Accept/reject rent request for product",
        description = "Endpoint to accept or reject a certain rent request (rr) for a given product. If accepted the according rent is added to product and rent request status is set accordingly"
    )
    @PostMapping("/products/{id}/rent/{rentRequestId}")
    fun acceptOrRejectRentRequest(
        @PathVariable(value = "id") productId: String,
        @PathVariable(value = "rentRequestId") rentRequestId: String,
        @RequestParam("isAccepted") isAccepted: Boolean,
        principal: Principal
    ): RentRequestView {
        log.info("Accept/reject rent request. {method=POST, endpoint=/products/{id}/rent/{rentRequestId}, productId=$productId, rentRequestId=$rentRequestId, isAccepted=$isAccepted, requesterUserId=${principal.name}}")
        return ObjectMapperUtils.map(
            productService.acceptOrRejectRentRequest(productId, rentRequestId, isAccepted, principal.name),
            RentRequestView::class.java
        )
    }

    @Operation(summary = "Create rent request")
    @PostMapping("/rentRequest")
    fun addRentRequest(@RequestBody rentRequestDTO: RentRequestDTO, principal: Principal): RentRequestView {
        log.info("Add rent request. {method=POST, endpoint=/rentRequest, requesterUserId=${principal.name}}")
        val currentUserId = principal.name
        val createdRentRequestModel = rentRequestService.create(
            ObjectMapperUtils.map(rentRequestDTO, RentRequestModel::class.java),
            currentUserId
        )
        return ObjectMapperUtils.map(createdRentRequestModel, RentRequestView::class.java)
    }

    @Operation(summary = "Delete certain rent request")
    @DeleteMapping("/rentRequest/{id}")
    fun deleteByRentRequestId(
        @PathVariable(value = "id") rentRequestId: String,
        principal: Principal
    ): ResponseEntity<Any> {
        rentRequestService.deleteById(rentRequestId, principal.name)
        log.info("Delete rent request by id. {method=POST, endpoint=/rentRequest, requesterUserId=${principal.name}}")
        return ResponseEntity<Any>(HttpStatus.OK)
    }
}
