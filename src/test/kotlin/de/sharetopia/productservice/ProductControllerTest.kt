package de.sharetopia.productservice

import RestResponsePage
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder
import com.amazonaws.services.cognitoidp.model.AuthFlowType
import com.amazonaws.services.cognitoidp.model.AuthenticationResultType
import com.amazonaws.services.cognitoidp.model.InitiateAuthRequest
import com.amazonaws.services.elasticache.model.User
import de.sharetopia.productservice.product.dto.*
import de.sharetopia.productservice.product.exception.ProductNotFoundException
import de.sharetopia.productservice.product.exception.RentRequestNotFoundException
import de.sharetopia.productservice.product.model.*
import de.sharetopia.productservice.product.repository.ElasticProductRepository
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.repository.RentRequestRepository
import de.sharetopia.productservice.product.repository.UserRepository
import de.sharetopia.productservice.product.util.GeoCoder
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.http.*
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
    private val testUser1Id = "204e1304-26f0-47b5-b353-cee12f4c8d34"

    private val initialProductModel: ProductModel = ProductModel(
        defaultProductId,
        title="Rennrad Rot",
        description="Das ist mein rotes Rennrad",
        ownerOfProductUserId="204e1304-26f0-47b5-b353-cee12f4c8d34",
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
        var authenticationResult: AuthenticationResultType?
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
        cognitoClient.shutdown()
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
        var secondProduct = initialProductModel.copy()
        secondProduct.id = ObjectId.get().toString()
        saveOneProduct(secondProduct)

        val responseType: ParameterizedTypeReference<List<ProductView>> =
            object : ParameterizedTypeReference<List<ProductView>>() {}

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<String>(headers)

        val response = restTemplate.exchange(
            getRootUrl()+"/products",
            HttpMethod.GET,
            entity,
            responseType
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertEquals(2, response.body?.size)
    }

    @Test
    fun `should return single product by id`() {
        saveOneProduct(initialProductModel)

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<String>(headers)

        val response = restTemplate.exchange(
            getRootUrl() + "/products/$defaultProductId",
            HttpMethod.GET,
            entity,
            ProductView::class.java
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertEquals(defaultProductId, response.body?.id)
    }

    @Test
    fun `should create new product`() {
        val productRequest = prepareProductRequest()

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<ProductDTO>(productRequest, headers)

        val response = restTemplate.exchange(
            getRootUrl()+"/products",
            HttpMethod.POST,
            entity,
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
        assertThat(productRequest.rentableDateRange).usingRecursiveComparison().isEqualTo(response.body?.rentableDateRange)
        assertThat(productRequest.rents).usingRecursiveComparison().isEqualTo(response.body?.rents)
    }


    @Test
    fun `should update existing product`() {
        saveOneProduct(initialProductModel)
        Thread.sleep(1000)
        val productRequest = prepareProductRequest()

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<ProductDTO>(productRequest, headers)

        val updateResponse = restTemplate.exchange(
            getRootUrl() + "/products/$defaultProductId",
            HttpMethod.PUT,
            entity,
            ProductView::class.java
        )

        assertEquals(200, updateResponse.statusCode.value())
        assertEquals(defaultProductId, updateResponse.body?.id)
        assertEquals(productRequest.description, updateResponse.body?.description)
        assertEquals(productRequest.title, updateResponse.body?.title)
        assertEquals(productRequest.tags, updateResponse.body?.tags)
        assertEquals(productRequest.price, updateResponse.body?.price)
        assertEquals(productRequest.address, updateResponse.body?.address)
        assertThat(productRequest.rentableDateRange).usingRecursiveComparison().isEqualTo(updateResponse.body?.rentableDateRange)
        assertThat(productRequest.rents).usingRecursiveComparison().isEqualTo(updateResponse.body?.rents)
    }

    @Test
    fun `should delete existing task`() {
        saveOneProduct(initialProductModel)

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<String>(headers)

        val deleteResponse = restTemplate.exchange(
            getRootUrl() + "/products/$defaultProductId",
            HttpMethod.DELETE,
            entity,
            ProductView::class.java
        )

        assertEquals(200, deleteResponse.statusCode.value())
        assertFalse(productRepository.findById(defaultProductId).isPresent)
    }

    @Test
    fun `should update single field of existing task`() {
        saveOneProduct(initialProductModel)
        Thread.sleep(1000)
        val titleOnlyProductDTO = ProductDTO("patched")

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<ProductDTO>(titleOnlyProductDTO,headers)

        val patchResponse = restTemplate.exchange(
            getRootUrl() + "/products/$defaultProductId",
            HttpMethod.PATCH,
            entity,
            ProductView::class.java
        )

        assertEquals(titleOnlyProductDTO.title, patchResponse.body?.title)
        assertEquals(initialProductModel.description, patchResponse.body?.description)
        assertEquals(initialProductModel.price, patchResponse.body?.price)
        assertEquals(initialProductModel.tags, patchResponse.body?.tags)
        assertEquals(initialProductModel.address, patchResponse.body?.address)
        assertThat(initialProductModel.rentableDateRange).usingRecursiveComparison().isEqualTo(patchResponse.body?.rentableDateRange)
        assertThat(initialProductModel.rents).usingRecursiveComparison().isEqualTo(patchResponse.body?.rents)
    }

    @Test
    fun `should return page of products with specified ids`() {
        saveOneProduct(initialProductModel)

        var secondProduct = initialProductModel.copy()
        secondProduct.id = ObjectId.get().toString()

        saveOneProduct(secondProduct)

        val responseType: ParameterizedTypeReference<RestResponsePage<ProductView>> =
            object : ParameterizedTypeReference<RestResponsePage<ProductView>>() {}

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<String>(headers)

        val response: ResponseEntity<RestResponsePage<ProductView>> = restTemplate.exchange(
            getRootUrl() + "/products/batch/$defaultProductId,${secondProduct.id}",
            HttpMethod.GET,
            entity,
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

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<String>(headers)

        val response: ResponseEntity<RestResponsePage<ProductView>> = restTemplate.exchange(
            getRootUrl() + "/products/findNearCity?term=$searchTerm&distance=$searchDistance&cityIdentifier=$postalCode",
            HttpMethod.GET,
            entity,
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

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<String>(headers)

        val response: ResponseEntity<RestResponsePage<ProductView>> = restTemplate.exchange(
            getRootUrl() + "/products/findNearCity?term=$searchTerm&distance=$searchDistance&cityIdentifier=$cityName",
            HttpMethod.GET,
            entity,
            responseType
        )

        val productViewList: List<ProductView> = response.body!!.content

        assertEquals(200, response.statusCode.value())
        assertEquals(defaultProductId,productViewList[0].id)
    }

    @Test
    fun `should return initial product by near search with city name and date range search`() {
        saveOneProduct(initialProductModel)

        var secondProduct = initialProductModel.copy()
        secondProduct.id = ObjectId.get().toString()
        secondProduct.location = listOf(9.1938525,48.8848654)

        val searchTerm = initialProductModel.title
        val searchDistance = 20
        val cityName = "Stuttgart-Vaihingen"
        val startDate = "2021-12-12"
        val endDate = "2021-12-20"

        saveOneProduct(secondProduct)

        val responseType: ParameterizedTypeReference<RestResponsePage<ProductView>> =
            object : ParameterizedTypeReference<RestResponsePage<ProductView>>() {}

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<String>(headers)

        val response: ResponseEntity<RestResponsePage<ProductView>> = restTemplate.exchange(
            getRootUrl() + "/products/findNearCity?term=$searchTerm&distance=$searchDistance&cityIdentifier=$cityName&startDate=$startDate&endDate=$endDate",
            HttpMethod.GET,
            entity,
            responseType
        )

        val productViewList: List<ProductView> = response.body!!.content

        assertEquals(200, response.statusCode.value())
        assertEquals(defaultProductId,productViewList[0].id)
    }

    @Test
    fun `should accept rent request by adding rent request as rent to product and change rentRequest status to accepted`() {
        saveOneProduct(initialProductModel)

        val createdRR = rentRequestRepository.save(RentRequestModel(
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = defaultProductId
        ))

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<String>(headers)
        val response = restTemplate.exchange(
            getRootUrl() + "/products/${defaultProductId}/rent/${createdRR.id}?isAccepted=true",
            HttpMethod.POST,
            entity,
            RentRequestView::class.java
        )

        val productInDatabase = productRepository.findById(defaultProductId).orElseThrow { ProductNotFoundException(defaultProductId) }
        val acceptedRentRequestInDatabase = rentRequestRepository.findById(createdRR.id).orElseThrow { RentRequestNotFoundException(createdRR.id)}
        assertEquals(200, response.statusCode.value())
        assertNotNull(productInDatabase.rents?.find {it.rentId == createdRR.id})
        assertEquals("accepted", acceptedRentRequestInDatabase.status)
    }

    @Test
    fun `should create new rent request`() {
        saveOneProduct(initialProductModel)
        val rentRequest = RentRequestDTO(
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requestedProductId = defaultProductId
        )

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<RentRequestDTO>(rentRequest, headers)

        val response = restTemplate.exchange(
            getRootUrl()+"/rentRequest",
            HttpMethod.POST,
            entity,
            RentRequestView::class.java
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertNotNull(response.body?.id)
        assertEquals(testUser1Id, response.body?.requesterUserId)
        assertEquals(rentRequest.fromDate, response.body?.fromDate)
        assertEquals(rentRequest.toDate, response.body?.toDate)
        assertEquals(initialProductModel.ownerOfProductUserId, response.body?.rentRequestReceiverUserId)
    }

    @Test
    fun `should delete existing rent request`() {
        saveOneProduct(initialProductModel)

        val rentRequestStoredInDatabase = rentRequestRepository.save(RentRequestModel(
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requestedProductId = defaultProductId,
            rentRequestReceiverUserId = testUser1Id,
            requesterUserId = testUser1Id
        ))

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<String>(headers)

        val deleteResponse = restTemplate.exchange(
            getRootUrl() + "/rentRequest/${rentRequestStoredInDatabase.id}",
            HttpMethod.DELETE,
            entity,
            String::class.java
        )

        assertEquals(200, deleteResponse.statusCode.value())
        assertFalse(rentRequestRepository.findById(rentRequestStoredInDatabase.id).isPresent)
    }

    @Test
    fun `should create new user`() {
        val userRequest = UserDTO(profilePictureURL="www.test.de/12312498", forename="Thomas", surname="test", postalCode="12345", address="test", city="test", rating="2")

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<UserDTO>(userRequest, headers)

        val response = restTemplate.exchange(
            getRootUrl()+"/user",
            HttpMethod.POST,
            entity,
            UserView::class.java
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertEquals(testUser1Id, response.body?.id)
        assertEquals(userRequest.profilePictureURL, response.body?.profilePictureURL)
        assertEquals(userRequest.forename, response.body?.forename)

    }

    @Test
    fun `should return current authorized user`(){
        val userToCreate = UserModel(id=testUser1Id, profilePictureURL="www.test.de/12312498", forename="Thomas", surname="test", postalCode="12345", address="test", city="test", rating="2")
        userRepository.save(userToCreate)

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<String>(headers)

        val response = restTemplate.exchange(
            getRootUrl()+"/user",
            HttpMethod.GET,
            entity,
            UserView::class.java
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertEquals(testUser1Id, response.body?.id)
        assertEquals(userToCreate.profilePictureURL, response.body?.profilePictureURL)
    }

    @Test
    fun `should return offered products of authorized user`(){
        saveOneProduct(initialProductModel)
        var secondProduct = initialProductModel.copy()
        secondProduct.id = ObjectId.get().toString()
        saveOneProduct(secondProduct)

        val rentRequestStoredInDatabase = rentRequestRepository.save(RentRequestModel(
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requestedProductId = defaultProductId,
            rentRequestReceiverUserId = testUser1Id,
            requesterUserId = testUser1Id
        ))


        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<String>(headers)

        val responseType: ParameterizedTypeReference<MutableList<UserProductsWithRentRequestsView>> =
            object : ParameterizedTypeReference<MutableList<UserProductsWithRentRequestsView>>() {}

        val response = restTemplate.exchange(
            getRootUrl()+"/user/offeredProductsOverview",
            HttpMethod.GET,
            entity,
            responseType
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertEquals(2, response.body?.size)
        assertTrue(response.body?.get(0)?.rentRequests!!.size==1)
        assertTrue(response.body?.get(1)?.rentRequests!!.isEmpty())
    }

    @Test
    fun `should return rent requests started by authorized user`(){
        saveOneProduct(initialProductModel)
        var secondProduct = initialProductModel.copy()
        secondProduct.id = ObjectId.get().toString()
        saveOneProduct(secondProduct)

        val rentRequestByAuthUserForProduct1StoredInDatabase = rentRequestRepository.save(RentRequestModel(
            fromDate = LocalDate.parse("2021-12-10", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-21", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requestedProductId = defaultProductId,
            rentRequestReceiverUserId = "1234",
            requesterUserId = testUser1Id
        ))

        val rentRequestByAuthUserForProduct2StoredInDatabase = rentRequestRepository.save(RentRequestModel(
            fromDate = LocalDate.parse("2021-12-22", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-27", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requestedProductId = secondProduct.id,
            rentRequestReceiverUserId = "1234",
            requesterUserId = testUser1Id
        ))

        val rentRequestByOtherUserForProduct1StoredInDatabase = rentRequestRepository.save(RentRequestModel(
            fromDate = LocalDate.parse("2021-12-22", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-27", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requestedProductId = defaultProductId,
            rentRequestReceiverUserId = "1234",
            requesterUserId = "5678"
        ))

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer "+login("tset123456@web.de","ackeracker123"))
        val entity = HttpEntity<String>(headers)

        val responseType: ParameterizedTypeReference<MutableList<UserSentRentRequestsWithProductsView>> =
            object : ParameterizedTypeReference<MutableList<UserSentRentRequestsWithProductsView>>() {}

        val response = restTemplate.exchange(
            getRootUrl()+"/user/requestedProductsOverview",
            HttpMethod.GET,
            entity,
            responseType
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertNotNull(response.body?.size==2)
        assertEquals(rentRequestByAuthUserForProduct1StoredInDatabase.id, response.body?.get(0)?.rentRequest?.id)
        assertEquals(defaultProductId, response.body?.get(0)?.product?.id)
        assertEquals(rentRequestByAuthUserForProduct2StoredInDatabase.id, response.body?.get(1)?.rentRequest?.id)
        assertEquals(secondProduct.id, response.body?.get(1)?.product?.id)

    }

    private fun getRootUrl(): String = "http://localhost:$port/api/v1"


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
