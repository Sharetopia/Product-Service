package de.sharetopia.productservice

import RestResponsePage
import de.sharetopia.productservice.product.dto.ProductDTO
import de.sharetopia.productservice.product.dto.ProductView
import de.sharetopia.productservice.product.model.*
import de.sharetopia.productservice.product.repository.ElasticProductRepository
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import org.bson.types.ObjectId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductControllerTest @Autowired constructor(
    private val productRepository: ProductRepository,
    private val elasticProductRepository: ElasticProductRepository,
    private val restTemplate: TestRestTemplate
){
    private val defaultProductId = ObjectId.get().toString()

    private val initialProductModel: ProductModel = ProductModel(
        ObjectId(defaultProductId),
        title="This is a Title that is interesting",
        description="Description",
        tags=listOf("Tags"),
        address = Address("Nobelstraße 10","Stuttgart", "70569"),
        location = listOf(9.100591,48.7419328),
        rentableDateRange = DateRangeDuration(
            LocalDate.parse("2021-10-10", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            LocalDate.parse("2022-04-10", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        ),
        rents = mutableListOf(
            Rent("3242354",
                DateRangeDuration(
                    LocalDate.parse("2021-10-11", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    LocalDate.parse("2021-10-16", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                ),
                "2142423535"
            )
        )
    )

    @LocalServerPort
    protected var port: Int = 0

    @BeforeEach
    fun setUp() {
        productRepository.deleteAll()
        elasticProductRepository.deleteAll()
    }

    @Test
    fun `should return all products`() {
        saveOneProduct(initialProductModel)

        val response = restTemplate.getForEntity(
            getRootUrl(),
            List::class.java
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertEquals(1, response.body?.size)
    }

    @Test
    fun `should return single product by id`() {
        saveOneProduct(initialProductModel)

        val response = restTemplate.getForEntity(
            getRootUrl() + "/$defaultProductId",
            ProductView::class.java
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertEquals(defaultProductId, response.body?.id)
    }

    @Test
    fun `should create new product`() {
        val productRequest = prepareProductRequest()

        val response = restTemplate.postForEntity(
            getRootUrl(),
            productRequest,
            ProductView::class.java
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertNotNull(response.body?.id)
        assertEquals(productRequest.description, response.body?.description)
        assertEquals(productRequest.title, response.body?.title)
        assertEquals(productRequest.tags, response.body?.tags)
        assertEquals(productRequest.tags, response.body?.tags)
    }


    @Test
    fun `should update existing product`() {
        saveOneProduct(initialProductModel)
        val productRequest = prepareProductRequest()

        val updateResponse = restTemplate.exchange(
            getRootUrl() + "/$defaultProductId",
            HttpMethod.PUT,
            HttpEntity(productRequest, HttpHeaders()),
            ProductView::class.java
        )


        assertEquals(200, updateResponse.statusCode.value())
        assertEquals(defaultProductId, updateResponse.body?.id)
        assertEquals(productRequest.description, updateResponse.body?.description)
        assertEquals(productRequest.title, updateResponse.body?.title)
        assertEquals(productRequest.tags, updateResponse.body?.tags)
    }

    @Test
    fun `should delete existing task`() {
        saveOneProduct(initialProductModel)

        val delete = restTemplate.exchange(
            getRootUrl() + "/$defaultProductId",
            HttpMethod.DELETE,
            HttpEntity(null, HttpHeaders()),
            ResponseEntity::class.java
        )

        assertEquals(200, delete.statusCode.value())
        assertFalse(productRepository.findById(defaultProductId).isPresent)
    }

    @Test
    fun `should update single field of existing task`() {
        saveOneProduct(initialProductModel)
        val titleOnlyProductDTO = ProductDTO("patched")

        val response = restTemplate.patchForObject(
            getRootUrl() + "/$defaultProductId",
            titleOnlyProductDTO,
            ProductView::class.java
        )

        assertEquals(titleOnlyProductDTO.title, response.title)
        assertEquals(initialProductModel.description, response.description)
        assertEquals(initialProductModel.tags, response.tags)
    }

    @Test
    fun `should return page of products with specified ids`() {
        saveOneProduct(initialProductModel)

        var secondProduct = initialProductModel.copy()
        secondProduct.id = ObjectId.get()

        saveOneProduct(secondProduct)

        val responseType: ParameterizedTypeReference<RestResponsePage<ProductView>> =
            object : ParameterizedTypeReference<RestResponsePage<ProductView>>() {}

        val response: ResponseEntity<RestResponsePage<ProductView>> = restTemplate.exchange(
            getRootUrl() + "/batch/$defaultProductId,${secondProduct.id}",
            HttpMethod.GET,
            null,
            responseType
        )

        val productViewList: List<ProductView> = response.body!!.content

        assertEquals(200, response.statusCode.value())
        assertEquals(defaultProductId,productViewList[0].id)
        assertEquals(secondProduct.id.toString(),productViewList[1].id)
        assertEquals(2, productViewList.size)
    }

    @Test
    fun `should return initial product by near search`() {
        saveOneProduct(initialProductModel)

        var secondProduct = initialProductModel.copy()
        secondProduct.id = ObjectId.get()
        secondProduct.location = listOf(9.1938525,48.8848654)

        saveOneProduct(secondProduct)

        val responseType: ParameterizedTypeReference<RestResponsePage<ProductView>> =
            object : ParameterizedTypeReference<RestResponsePage<ProductView>>() {}

        val response: ResponseEntity<RestResponsePage<ProductView>> = restTemplate.exchange(
            getRootUrl() + "/findNearCity?term=Title&distance=10&cityIdentifier=70569",
            HttpMethod.GET,
            null,
            responseType
        )

        val productViewList: List<ProductView> = response.body!!.content

        assertEquals(200, response.statusCode.value())
        assertEquals(defaultProductId,productViewList[0].id)
    }

    private fun getRootUrl(): String = "http://localhost:$port/api/v1/products"

    private fun saveOneProduct(productModel: ProductModel){
        val createdProductModel = productRepository.save(productModel)
        elasticProductRepository.save(ObjectMapperUtils.map(createdProductModel, ElasticProductModel::class.java))
    }

    private fun prepareProductRequest() = ProductDTO(
        title="Default title",
        description = "Default description",
        tags=listOf("default tag1", "default tag2"),
        address=Address("Nobelstraße 10","Stuttgart", "70569"),
        location = listOf(0.0,0.0),
        rentableDateRange = DateRangeDuration(
            LocalDate.parse("2021-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            LocalDate.parse("2025-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        ),
        rents = mutableListOf(
            Rent("1234",
                    DateRangeDuration(
                        LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    ),
                "235423532453453"
                )
            )
    )

}
