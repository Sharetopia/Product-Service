package de.sharetopia.productservice.integrationTests

import de.sharetopia.productservice.product.dto.*
import de.sharetopia.productservice.product.model.*
import de.sharetopia.productservice.product.repository.ElasticProductRepository
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.repository.RentRequestRepository
import de.sharetopia.productservice.product.repository.UserRepository
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import de.sharetopia.productservice.testUtil.AuthorizationUtils
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
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserControllerTest @Autowired constructor(
    private val productRepository: ProductRepository,
    private val elasticProductRepository: ElasticProductRepository,
    private val rentRequestRepository: RentRequestRepository,
    private val userRepository: UserRepository,
    private val restTemplate: TestRestTemplate
) {

    private val authorizationUtils = AuthorizationUtils()
    private val defaultProductId = ObjectId.get().toString()
    private val testUser1Id = "204e1304-26f0-47b5-b353-cee12f4c8d34"
    private val accessToken = authorizationUtils.login("tset123456@web.de", "ackeracker123")

    private val initialProductModel: ProductModel = ProductModel(
        defaultProductId,
        title = "Rennrad Rot",
        description = "Das ist mein rotes Rennrad",
        ownerOfProductUserId = "204e1304-26f0-47b5-b353-cee12f4c8d34",
        tags = listOf("Fahrrad", "Mobilität"),
        price = BigDecimal(12.99),
        address = Address("Nobelstraße 10", "Stuttgart", "70569"),
        location = listOf(9.100591, 48.7419328),
        rentableDateRange = DateRangeDuration(
            LocalDate.parse("2021-10-10", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            LocalDate.parse("2022-04-10", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        ),
        rents = mutableListOf(
            Rent(
                "3242354",
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
        rentRequestRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `should create new user`() {
        val userRequest = UserDTO(
            profilePictureURL = "www.test.de/12312498",
            forename = "Thomas",
            surname = "test",
            postalCode = "12345",
            address = "test",
            city = "test",
            rating = "2"
        )

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer $accessToken")
        val entity = HttpEntity<UserDTO>(userRequest, headers)

        val response = restTemplate.exchange(
            getRootUrl() + "/user",
            HttpMethod.POST,
            entity,
            UserView::class.java
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertEquals(testUser1Id, response.body?.id)
        assertEquals(userRequest.profilePictureURL, response.body?.profilePictureURL)
        assertEquals(userRequest.forename, response.body?.forename)
        assertEquals(userRequest.surname, response.body?.surname)
        assertEquals(userRequest.postalCode, response.body?.postalCode)
        assertEquals(userRequest.address, response.body?.address)
        assertEquals(userRequest.city, response.body?.city)
        assertEquals(userRequest.rating, response.body?.rating)

    }

    @Test
    fun `should update existing user`() {
        val userToCreate = UserModel(
            id = testUser1Id,
            profilePictureURL = "www.test.de/12312498",
            forename = "Thomas",
            surname = "test",
            postalCode = "12345",
            address = "test",
            city = "test",
            rating = "2"
        )
        userRepository.save(userToCreate)

        val userRequest = UserDTO(
            profilePictureURL = "www.test.de/12312498",
            forename = "Max",
            surname = "Mustermann",
            postalCode = "12345",
            address = "test",
            city = "test",
            rating = "2"
        )

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer $accessToken")
        val entity = HttpEntity<UserDTO>(userRequest, headers)

        val response = restTemplate.exchange(
            getRootUrl() + "/user",
            HttpMethod.PUT,
            entity,
            UserView::class.java
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertEquals(testUser1Id, response.body?.id)
        assertEquals(userRequest.profilePictureURL, response.body?.profilePictureURL)
        assertEquals(userRequest.forename, response.body?.forename)
        assertEquals(userRequest.surname, response.body?.surname)
        assertEquals(userRequest.postalCode, response.body?.postalCode)
        assertEquals(userRequest.address, response.body?.address)
        assertEquals(userRequest.city, response.body?.city)
        assertEquals(userRequest.rating, response.body?.rating)
    }

    @Test
    fun `should update single field of existing user`() {
        val userToCreate = UserModel(
            id = testUser1Id,
            profilePictureURL = "www.test.de/12312498",
            forename = "Thomas",
            surname = "test",
            postalCode = "12345",
            address = "test",
            city = "test",
            rating = "2"
        )
        userRepository.save(userToCreate)
        val profilePictureURLOnlyUserDTO = UserDTO("www.neueURL.de/123")

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer $accessToken")
        val entity = HttpEntity<UserDTO>(profilePictureURLOnlyUserDTO, headers)

        val patchResponse = restTemplate.exchange(
            getRootUrl() + "/user",
            HttpMethod.PATCH,
            entity,
            UserView::class.java
        )

        assertEquals(profilePictureURLOnlyUserDTO.profilePictureURL, patchResponse.body?.profilePictureURL)
        assertNotNull(patchResponse.body)
        assertEquals(testUser1Id, patchResponse.body?.id)
        assertEquals(userToCreate.forename, patchResponse.body?.forename)
        assertEquals(userToCreate.surname, patchResponse.body?.surname)
        assertEquals(userToCreate.postalCode, patchResponse.body?.postalCode)
        assertEquals(userToCreate.address, patchResponse.body?.address)
        assertEquals(userToCreate.city, patchResponse.body?.city)
        assertEquals(userToCreate.rating, patchResponse.body?.rating)
    }

    @Test
    fun `should return FORBIDDEN for trying to create user data as unauthorized user`() {
        val invalidAccessToken = "12345"

        val userRequest = UserDTO(
            profilePictureURL = "www.test.de/12312498",
            forename = "Max",
            surname = "Mustermann",
            postalCode = "12345",
            address = "test",
            city = "test",
            rating = "2"
        )


        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer $invalidAccessToken")
        val entity = HttpEntity<UserDTO>(userRequest, headers)

        val response = restTemplate.exchange(
            getRootUrl() + "/user",
            HttpMethod.POST,
            entity,
            UserView::class.java
        )

        assertEquals(403, response.statusCode.value())
    }

    @Test
    fun `should return current authorized user`() {
        val userToCreate = UserModel(
            id = testUser1Id,
            profilePictureURL = "www.test.de/12312498",
            forename = "Thomas",
            surname = "test",
            postalCode = "12345",
            address = "test",
            city = "test",
            rating = "2"
        )
        userRepository.save(userToCreate)

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer $accessToken")
        val entity = HttpEntity<String>(headers)

        val response = restTemplate.exchange(
            getRootUrl() + "/user",
            HttpMethod.GET,
            entity,
            UserView::class.java
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertEquals(testUser1Id, response.body?.id)
        assertEquals(userToCreate.profilePictureURL, response.body?.profilePictureURL)
        assertEquals(userToCreate.forename, response.body?.forename)
        assertEquals(userToCreate.surname, response.body?.surname)
        assertEquals(userToCreate.postalCode, response.body?.postalCode)
        assertEquals(userToCreate.address, response.body?.address)
        assertEquals(userToCreate.city, response.body?.city)
        assertEquals(userToCreate.rating, response.body?.rating)
    }


    @Test
    fun `should return offered products of authorized user`() {
        saveOneProduct(initialProductModel)
        var secondProduct = initialProductModel.copy()
        secondProduct.id = ObjectId.get().toString()
        saveOneProduct(secondProduct)

        rentRequestRepository.save(
            RentRequestModel(
                fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                requestedProductId = defaultProductId,
                rentRequestReceiverUserId = testUser1Id,
                requesterUserId = testUser1Id
            )
        )


        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer $accessToken")
        val entity = HttpEntity<String>(headers)

        val responseType: ParameterizedTypeReference<MutableList<UserProductsWithRentRequestsView>> =
            object : ParameterizedTypeReference<MutableList<UserProductsWithRentRequestsView>>() {}

        val response = restTemplate.exchange(
            getRootUrl() + "/user/offeredProductsOverview",
            HttpMethod.GET,
            entity,
            responseType
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertEquals(2, response.body?.size)
        assertTrue(response.body?.get(0)?.rentRequests!!.size == 1)
        assertTrue(response.body?.get(1)?.rentRequests!!.isEmpty())
    }

    @Test
    fun `should return rent requests started by authorized user`() {
        saveOneProduct(initialProductModel)
        var secondProduct = initialProductModel.copy()
        secondProduct.id = ObjectId.get().toString()
        saveOneProduct(secondProduct)

        val rentRequestByAuthUserForProduct1StoredInDatabase = rentRequestRepository.save(
            RentRequestModel(
                fromDate = LocalDate.parse("2021-12-10", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                toDate = LocalDate.parse("2021-12-21", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                requestedProductId = defaultProductId,
                rentRequestReceiverUserId = "1234",
                requesterUserId = testUser1Id
            )
        )

        val rentRequestByAuthUserForProduct2StoredInDatabase = rentRequestRepository.save(
            RentRequestModel(
                fromDate = LocalDate.parse("2021-12-22", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                toDate = LocalDate.parse("2021-12-27", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                requestedProductId = secondProduct.id,
                rentRequestReceiverUserId = "1234",
                requesterUserId = testUser1Id
            )
        )

        rentRequestRepository.save(
            RentRequestModel(
                fromDate = LocalDate.parse("2021-12-22", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                toDate = LocalDate.parse("2021-12-27", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                requestedProductId = defaultProductId,
                rentRequestReceiverUserId = "1234",
                requesterUserId = "5678"
            )
        )

        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer $accessToken")
        val entity = HttpEntity<String>(headers)

        val responseType: ParameterizedTypeReference<MutableList<UserSentRentRequestsWithProductsView>> =
            object : ParameterizedTypeReference<MutableList<UserSentRentRequestsWithProductsView>>() {}

        val response = restTemplate.exchange(
            getRootUrl() + "/user/requestedProductsOverview",
            HttpMethod.GET,
            entity,
            responseType
        )

        assertEquals(200, response.statusCode.value())
        assertNotNull(response.body)
        assertNotNull(response.body?.size == 2)
        assertEquals(rentRequestByAuthUserForProduct1StoredInDatabase.id, response.body?.get(0)?.rentRequest?.id)
        assertEquals(defaultProductId, response.body?.get(0)?.product?.id)
        assertEquals(rentRequestByAuthUserForProduct2StoredInDatabase.id, response.body?.get(1)?.rentRequest?.id)
        assertEquals(secondProduct.id, response.body?.get(1)?.product?.id)

    }

    private fun getRootUrl(): String = "http://localhost:$port/api/v1"


    private fun saveOneProduct(productModel: ProductModel) {
        val createdProductModel = productRepository.save(productModel)
        elasticProductRepository.save(ObjectMapperUtils.map(createdProductModel, ElasticProductModel::class.java))
    }

    private fun prepareProductRequest() = ProductDTO(
        title = "Piaggio Roller",
        description = "Das ist mein weißer Piaggio Roller",
        tags = listOf("Mobilität", "Roller"),
        price = BigDecimal(10.99),
        address = Address("Obere Bahnhofstraße 1", "Backnang", "71522"),
        rentableDateRange = DateRangeDuration(
            LocalDate.parse("2021-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            LocalDate.parse("2025-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        ),
        rents = mutableListOf(
            Rent(
                "1234",
                DateRangeDuration(
                    LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                ),
                "235423532453453"
            )
        )
    )

}
