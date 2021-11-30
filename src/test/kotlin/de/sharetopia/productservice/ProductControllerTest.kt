package de.sharetopia.productservice

import de.sharetopia.productservice.product.dto.ProductDTO
import de.sharetopia.productservice.product.dto.ProductView
import de.sharetopia.productservice.product.exception.ProductNotFoundException
import de.sharetopia.productservice.product.model.ProductModel
import de.sharetopia.productservice.product.repository.ProductRepository
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
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit.jupiter.SpringExtension


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductControllerTest @Autowired constructor(
    private val productRepository: ProductRepository,
    private val restTemplate: TestRestTemplate
){
    private val defaultProductId = ObjectId.get().toString()

    @LocalServerPort
    protected var port: Int = 0

    @BeforeEach
    fun setUp() {
        productRepository.deleteAll()
    }

    @Test
    fun `should return all products`() {
        saveOneProduct()

        val response = restTemplate.getForEntity(
            getRootUrl(),
            List::class.java
        )

        println(response.body)
        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        //assertEquals(1, response.body?.size)
    }

    @Test
    fun `should return single product by id`() {
        saveOneProduct()

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
        //assertNotNull(response.body?.id)
        assertEquals(productRequest.description, response.body?.description)
        assertEquals(productRequest.title, response.body?.title)
        assertEquals(productRequest.tags, response.body?.tags)
    }


    @Test
    fun `should update existing product`() {
        saveOneProduct()
        val productRequest = prepareProductRequest()

        val updateResponse = restTemplate.exchange(
            getRootUrl() + "/$defaultProductId",
            HttpMethod.PUT,
            HttpEntity(productRequest, HttpHeaders()),
            ProductView::class.java
        )

        //val updatedProduct: Optional<ProductModel> = productRepository.findById(defaultProductId)

        assertEquals(200, updateResponse.statusCode.value())
        /*assertEquals(defaultProductId, updatedProduct.id)
        assertEquals(productRequest.description, updatedProduct.description)
        assertEquals(productRequest.title, updatedProduct.title)*/
    }

    @Test
    fun `should delete existing task`() {
        saveOneProduct()

        val delete = restTemplate.exchange(
            getRootUrl() + "/$defaultProductId",
            HttpMethod.DELETE,
            HttpEntity(null, HttpHeaders()),
            ResponseEntity::class.java
        )

        assertEquals(200, delete.statusCode.value())
        assertFalse(productRepository.findById(defaultProductId).isPresent)
    }


    private fun getRootUrl(): String? = "http://localhost:$port/api/v1/products"

    private fun saveOneProduct() = productRepository.save(ProductModel(ObjectId(defaultProductId), "Title", "Description", listOf("Tags")))

    private fun prepareProductRequest() = ProductDTO(title="Default title", description = "Default description", tags=listOf("default tag1", "default tag2"))

}