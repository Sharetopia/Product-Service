package de.sharetopia.productservice

import RestResponsePage
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.auth.profile.internal.ProfileKeyConstants.REGION
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClientBuilder
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder
import com.amazonaws.services.cognitoidp.model.AdminInitiateAuthRequest
import com.amazonaws.services.cognitoidp.model.AuthFlowType
import com.amazonaws.services.cognitoidp.model.AuthenticationResultType
import com.amazonaws.services.cognitoidp.model.InitiateAuthRequest
import de.sharetopia.productservice.product.dto.ProductDTO
import de.sharetopia.productservice.product.dto.ProductView
import de.sharetopia.productservice.product.model.*
import de.sharetopia.productservice.product.repository.ElasticProductRepository
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.repository.RentRequestRepository
import de.sharetopia.productservice.product.repository.UserRepository
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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductControllerTest @Autowired constructor(
    private val productRepository: ProductRepository,
    private val elasticProductRepository: ElasticProductRepository,
    private val rentRequestRepository: RentRequestRepository,
    private val userRepository: UserRepository,
    private val restTemplate: TestRestTemplate
){
    private val defaultProductId = ObjectId.get().toString()

    private val initialProductModel: ProductModel = ProductModel(
        defaultProductId,
        title="Rennrad Rot",
        description="Das ist mein rotes Rennrad",
        ownerOfProductUserId="1234",
        tags=listOf("Fahrrad", "Mobilität"),
        price= BigDecimal(12.99),
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

    val testRegion = "eu-central-1"
    val testUserPoolId = "eu-central-1_sxdpyi2QD"
    val testClientId = "3vgfb9n80chgbah3lvqiq7l9f0"

    fun getAmazonCognitoIdentityClient(): AWSCognitoIdentityProvider? {
        val awsCreds = AnonymousAWSCredentials()
        val provider = AWSCognitoIdentityProviderClientBuilder
            .standard()
            .withCredentials(AWSStaticCredentialsProvider(awsCreds))
            .withRegion(testRegion)
            .build()
        return provider
    }

    fun login(username: String, password: String): String {
        var authenticationResult: AuthenticationResultType? = null
        val cognitoClient = getAmazonCognitoIdentityClient()

        val authParams: MutableMap<String, String> = HashMap()
        authParams["USERNAME"] = username
        authParams["PASSWORD"] = password

        val authRequest = InitiateAuthRequest()
        authRequest.withAuthFlow(AuthFlowType.USER_PASSWORD_AUTH)
            .withClientId(testClientId)
            .withAuthParameters(authParams)

        val result = cognitoClient!!.initiateAuth(authRequest)


        authenticationResult = result.authenticationResult
        //cognitoClient.shutdown()
        return authenticationResult.accessToken
    }

    @BeforeEach
    fun setUp() {
        productRepository.deleteAll()
        elasticProductRepository.deleteAll()
        rentRequestRepository.deleteAll()
        userRepository.deleteAll()
        Thread.sleep(1000) //TODO remove, currently there because geocoding api allows only max 1 request per second
    }

    @Test
    fun `should return all products`() {
        saveOneProduct(initialProductModel)
        Thread.sleep(1000)
        saveOneProduct(initialProductModel)

        val response =""
        /*val response = restTemplate.getForEntity(
            getRootUrl(),
            List::class.java
        )*/

        assertEquals(200, login("tset123456@web.de","ackeracker123"))
        //assertEquals(200, response.statusCode.value())
        //assertNotNull(response.body)
        //assertEquals(2, response.body?.size)
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
        assertEquals(productRequest.price, response.body?.price)
        assertEquals(productRequest.address, response.body?.address)
        assertEquals(productRequest.rentableDateRange, response.body?.rentableDateRange)
        assertEquals(productRequest.rents, response.body?.rents)
    }


    @Test
    fun `should update existing product`() {
        saveOneProduct(initialProductModel)
        Thread.sleep(1000)
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
        assertEquals(productRequest.price, updateResponse.body?.price)
        assertEquals(productRequest.address, updateResponse.body?.address)
        assertEquals(productRequest.rentableDateRange, updateResponse.body?.rentableDateRange)
        assertEquals(productRequest.rents, updateResponse.body?.rents)
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
        Thread.sleep(1000)
        val titleOnlyProductDTO = ProductDTO("patched")

        val response = restTemplate.patchForObject(
            getRootUrl() + "/$defaultProductId",
            titleOnlyProductDTO,
            ProductView::class.java
        )

        assertEquals(titleOnlyProductDTO.title, response.title)
        assertEquals(initialProductModel.description, response.description)
        assertEquals(initialProductModel.price, response.price)
        assertEquals(initialProductModel.tags, response.tags)
        assertEquals(initialProductModel.address, response.address)
        assertEquals(initialProductModel.rentableDateRange, response.rentableDateRange)
        assertEquals(initialProductModel.rents, response.rents)
    }

    @Test
    fun `should return page of products with specified ids`() {
        saveOneProduct(initialProductModel)

        var secondProduct = initialProductModel.copy()
        secondProduct.id = ObjectId.get().toString()

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
        assertEquals(secondProduct.id,productViewList[1].id)
        assertEquals(2, productViewList.size)
    }

    @Test
    fun `should return initial product by near search with postal code`() {
        saveOneProduct(initialProductModel)
        Thread.sleep(1000)

        var secondProduct = initialProductModel.copy()
        secondProduct.id = ObjectId.get().toString()
        secondProduct.location = listOf(9.1938525,48.8848654)

        val searchTerm = initialProductModel.title
        val searchDistance = 10
        val postalCode = 70569

        saveOneProduct(secondProduct)
        Thread.sleep(1000)

        val responseType: ParameterizedTypeReference<RestResponsePage<ProductView>> =
            object : ParameterizedTypeReference<RestResponsePage<ProductView>>() {}

        val response: ResponseEntity<RestResponsePage<ProductView>> = restTemplate.exchange(
            getRootUrl() + "/findNearCity?term=$searchTerm&distance=$searchDistance&cityIdentifier=$postalCode",
            HttpMethod.GET,
            null,
            responseType
        )

        val productViewList: List<ProductView> = response.body!!.content

        assertEquals(200, response.statusCode.value())
        assertEquals(defaultProductId,productViewList[0].id)
    }

    @Test
    fun `should return initial product by near search with city name`() {
        saveOneProduct(initialProductModel)

        var secondProduct = initialProductModel.copy()
        secondProduct.id = ObjectId.get().toString()
        secondProduct.location = listOf(9.1938525,48.8848654)

        val searchTerm = initialProductModel.title
        val searchDistance = 20
        val cityName = "Stuttgart-Vaihingen"

        saveOneProduct(secondProduct)

        val responseType: ParameterizedTypeReference<RestResponsePage<ProductView>> =
            object : ParameterizedTypeReference<RestResponsePage<ProductView>>() {}

        val response: ResponseEntity<RestResponsePage<ProductView>> = restTemplate.exchange(
            getRootUrl() + "/findNearCity?term=$searchTerm&distance=$searchDistance&cityIdentifier=$cityName",
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
        title="Piaggio Roller",
        description = "Das ist mein weißer Piaggio Roller",
        tags=listOf("Mobilität", "Roller"),
        price=BigDecimal(10.99),
        address=Address("Obere Bahnhofstraße 1","Backnang", "71522"),
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
