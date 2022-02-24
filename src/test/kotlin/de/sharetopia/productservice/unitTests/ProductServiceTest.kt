package de.sharetopia.productservice.unitTests

import RestResponsePage
import de.sharetopia.productservice.product.dto.ProductDTO
import de.sharetopia.productservice.product.exception.LocationNotFoundException
import de.sharetopia.productservice.product.exception.NotAllowedAccessToResourceException
import de.sharetopia.productservice.product.exception.ProductNotFoundException
import de.sharetopia.productservice.product.exception.RentRequestNotFoundException
import de.sharetopia.productservice.product.model.*
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.repository.RentRequestRepository
import de.sharetopia.productservice.product.service.ElasticProductService
import de.sharetopia.productservice.product.service.ProductService
import de.sharetopia.productservice.product.service.ProductServiceImpl
import de.sharetopia.productservice.product.service.RentRequestService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductServiceTest {

    @Mock
    lateinit var productRepository: ProductRepository

    @Mock
    lateinit var elasticProductService: ElasticProductService

    @Mock
    lateinit var rentRequestService: RentRequestService

    @Mock
    lateinit var rentRequestRepository: RentRequestRepository

    @InjectMocks
    var productService: ProductService = ProductServiceImpl()

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `should return all products as a list`() {
        val productList = listOf(
            ProductModel(
                id = "12345",
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
            ),
            ProductModel(
                id = "5678",
                title = "Auto",
                description = "Mein tolles neues Auto hat Bremse, Hupe und Licht.",
                tags = listOf("Fahrrad", "Mobilität"),
                address = Address("Ludwigsburger Straße 11", "Backnang", "71522"),
                //format is lng-lat
                location = listOf(9.430380, 48.923069),
                rentableDateRange = DateRangeDuration(
                    LocalDate.parse("2021-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    LocalDate.parse("2025-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                ),
                rents = mutableListOf(
                    Rent(
                        "214234235",
                        DateRangeDuration(
                            LocalDate.parse("2021-11-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            LocalDate.parse("2021-12-15", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        ),
                        "12432342353425345213"
                    ),
                    Rent(
                        "5324234",
                        DateRangeDuration(
                            LocalDate.parse("2021-10-10", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            LocalDate.parse("2021-10-20", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        ),
                        "234210230575"
                    )
                )
            )
        )
        `when`(productRepository.findAll()).thenReturn(productList)

        //test
        val productListReturnedByService = productService.findAll()

        assertEquals(2, productListReturnedByService.size)
        assertEquals("12345", productListReturnedByService[0].id)
        assertEquals("5678", productListReturnedByService[1].id)
        verify(productRepository, times(1)).findAll()
    }

    @Test
    fun `should return created product`() {
        val productToCreate = ProductModel(
            id = "12345",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "204e1304-26f0-47b5-b353-cee12f4c8d34",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        whenever(productRepository.insert(productToCreate)).thenReturn(productToCreate)

        //test
        val productReturnedByService =
            productService.create(productToCreate, userId = "204e1304-26f0-47b5-b353-cee12f4c8d34")

        verify(productRepository, times(1)).insert(productToCreate)
        verify(elasticProductService, times(1)).save(any<ElasticProductModel>())

        assertEquals(productToCreate.title, productReturnedByService.title)
        assertEquals(productToCreate.description, productReturnedByService.description)
        assertThat(productToCreate.address).usingRecursiveComparison().isEqualTo(productReturnedByService.address)
        assertTrue((9.10 < productReturnedByService.location!![0]) && (productReturnedByService.location!![0] < 9.11))
        assertTrue(
            (48.74 < productReturnedByService.location!![1]) && (productReturnedByService.location!![1] < 48.75)
        )
    }

    @Test
    fun `should update existing product by calling updateOrInsert for existing product`() {
        val mockedProductInDb =
            ProductModel(
                id = "12345",
                title = "Rennrad Rot",
                description = "Das ist mein rotes Rennrad",
                ownerOfProductUserId = "204e1304-26f0-47b5-b353-cee12f4c8d34",
                tags = listOf("Fahrrad", "Mobilität"),
                price = BigDecimal(12.99),
                address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        val updateProduct =
            ProductModel(
                title = "Rennrad Blau",
                description = "Das ist mein blaues Rennrad",
                ownerOfProductUserId = "204e1304-26f0-47b5-b353-cee12f4c8d34",
                tags = listOf("Fahrrad", "Mobilität"),
                price = BigDecimal(12.99),
                address = Address("Ludwigsburger Straße 11", "Backnang", "71522"),
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

        `when`(productRepository.findById(anyString())).thenReturn(Optional.of(mockedProductInDb))
        whenever(productRepository.save(any<ProductModel>())).doAnswer { it.arguments[0] as ProductModel }

        //test
        val productReturnedByService =
            productService.updateOrInsert("12345", updateProduct, userId = "204e1304-26f0-47b5-b353-cee12f4c8d34")

        verify(productRepository, times(1)).save(any<ProductModel>())
        verify(elasticProductService, times(1)).save(any<ElasticProductModel>())

        assertEquals(updateProduct.title, productReturnedByService.title)
        assertEquals(updateProduct.description, productReturnedByService.description)
        assertThat(updateProduct.address).usingRecursiveComparison().isEqualTo(productReturnedByService.address)
        assertTrue((9.43 < productReturnedByService.location?.get(0)!!) && (productReturnedByService.location?.get(0)!! < 9.44))
        assertTrue(
            (48.97 < productReturnedByService.location?.get(1)!!) && (productReturnedByService.location?.get(
                1
            )!! < 48.98)
        )
    }

    @Test
    fun `should throw NotAllowedAccessToResourceException trying to update product of other user`() {
        val mockedProductInDb =
            ProductModel(
                id = "12345",
                title = "Rennrad Rot",
                description = "Das ist mein rotes Rennrad",
                ownerOfProductUserId = "204e1304-26f0-47b5-b353-cee12f4c8d34",
                tags = listOf("Fahrrad", "Mobilität"),
                price = BigDecimal(12.99),
                address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        val updateProduct =
            ProductModel(
                title = "Rennrad Blau",
                description = "Das ist mein blaues Rennrad",
                ownerOfProductUserId = "204e1304-26f0-47b5-b353-cee12f4c8d34",
                tags = listOf("Fahrrad", "Mobilität"),
                price = BigDecimal(12.99),
                address = Address("Ludwigsburger Straße 11", "Backnang", "71522"),
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

        `when`(productRepository.findById(anyString())).thenReturn(Optional.of(mockedProductInDb))
        whenever(productRepository.save(any<ProductModel>())).doAnswer { it.arguments[0] as ProductModel }

        assertThrows(NotAllowedAccessToResourceException::class.java) {
            productService.updateOrInsert(
                "12345",
                updateProduct,
                userId = "1234"
            )
        }
    }

    @Test
    fun `should throw LocationNotFoundException trying to update product with nonsense location`() {
        val mockedProductInDb =
            ProductModel(
                id = "12345",
                title = "Rennrad Rot",
                description = "Das ist mein rotes Rennrad",
                ownerOfProductUserId = "204e1304-26f0-47b5-b353-cee12f4c8d34",
                tags = listOf("Fahrrad", "Mobilität"),
                price = BigDecimal(12.99),
                address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        val updateProduct =
            ProductModel(
                title = "Rennrad Blau",
                description = "Das ist mein blaues Rennrad",
                ownerOfProductUserId = "204e1304-26f0-47b5-b353-cee12f4c8d34",
                tags = listOf("Fahrrad", "Mobilität"),
                price = BigDecimal(12.99),
                address = Address("sdgdsg", "dfsagadsg", "fdghhg"),
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

        `when`(productRepository.findById(anyString())).thenReturn(Optional.of(mockedProductInDb))
        whenever(productRepository.save(any<ProductModel>())).doAnswer { it.arguments[0] as ProductModel }

        assertThrows(LocationNotFoundException::class.java) {
            productService.updateOrInsert(
                "12345",
                updateProduct,
                userId = "204e1304-26f0-47b5-b353-cee12f4c8d34"
            )
        }
    }

    @Test
    fun `should return updated product after partial update`() {
        val mockedProductInDb =
            ProductModel(
                id = "12345",
                title = "Rennrad Rot",
                description = "Das ist mein rotes Rennrad",
                ownerOfProductUserId = "204e1304-26f0-47b5-b353-cee12f4c8d34",
                tags = listOf("Fahrrad", "Mobilität"),
                price = BigDecimal(12.99),
                address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        val updateFieldsProduct =
            ProductDTO(
                title = "Rennrad Blau",
                description = "Das ist mein blaues Rennrad",
                address = Address("Ludwigsburger Straße 11", "Backnang", "71522")
            )

        whenever(productRepository.findById("12345")).thenReturn(Optional.of(mockedProductInDb))
        whenever(productRepository.save(any<ProductModel>())).doAnswer { it.arguments[0] as ProductModel }

        //test
        val productReturnedByService =
            productService.partialUpdate("12345", updateFieldsProduct, "204e1304-26f0-47b5-b353-cee12f4c8d34")

        verify(productRepository).save(argThat { productModel: ProductModel ->
            (productModel.id === "12345") &&
                    (productModel.title === updateFieldsProduct.title) &&
                    (productModel.description === updateFieldsProduct.description) &&
                    (productModel.ownerOfProductUserId === mockedProductInDb.ownerOfProductUserId)
        })

        assertTrue((9.43 < productReturnedByService.location?.get(0)!!) && (productReturnedByService.location?.get(0)!! < 9.44))
        assertTrue(
            (48.97 < productReturnedByService.location?.get(1)!!) && (productReturnedByService.location?.get(
                1
            )!! < 48.98)
        )

        verify(productRepository, times(1)).save(any<ProductModel>())
    }

    @Test
    fun `should return product by id`() {
        val productInDb = ProductModel(
            id = "12345",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "204e1304-26f0-47b5-b353-cee12f4c8d34",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            location = listOf(9.1938525, 48.8848654),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        `when`(productRepository.findById(productInDb.id)).thenReturn(Optional.of(productInDb))

        //test
        productService.findById("12345")
        verify(productRepository, times(1)).findById(productInDb.id)
    }

    @Test
    fun `should throw ProductNotFoundException when trying to access non-existing id`() {
        //test
        assertThrows(ProductNotFoundException::class.java) {
            productService.findById("12345")
        }
    }

    @Test
    fun `should delete product by id`() {
        val idOfProductToDelete = "12345"
        val productInDb = ProductModel(
            id = "12345",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "204e1304-26f0-47b5-b353-cee12f4c8d34",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            location = listOf(9.1938525, 48.8848654),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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
        whenever(productRepository.findById(productInDb.id)).thenReturn(Optional.of(productInDb))
        whenever(elasticProductService.deleteById(productInDb.id)).doAnswer { }

        productService.deleteById(idOfProductToDelete, "204e1304-26f0-47b5-b353-cee12f4c8d34")
        verify(productRepository, times(1)).deleteById(idOfProductToDelete)
        verify(elasticProductService, times(1)).deleteById(idOfProductToDelete)
    }

    @Test
    fun `should throw ProductNotFoundException when trying to delete product by non-existent id`() {
        assertThrows(ProductNotFoundException::class.java) {
            productService.findById("1111")
        }
    }

    @Test
    fun `should throw NotAllowedAccessToResourceException when trying to delete product of other user`() {
        val idOfProductToDelete = "12345"
        val productInDb = ProductModel(
            id = "12345",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "204e1304-26f0-47b5-b353-cee12f4c8d34",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            location = listOf(9.1938525, 48.8848654),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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
        whenever(productRepository.findById(productInDb.id)).thenReturn(Optional.of(productInDb))
        whenever(elasticProductService.deleteById(productInDb.id)).doAnswer { }

        assertThrows(NotAllowedAccessToResourceException::class.java) {
            productService.deleteById(idOfProductToDelete, "1111")
        }
    }

    @Test
    fun `find many by ids`() {
        val productList = listOf(
            ProductModel(
                id = "12345",
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
            ),
            ProductModel(
                id = "5678",
                title = "Auto",
                description = "Mein tolles neues Auto hat Bremse, Hupe und Licht.",
                tags = listOf("Fahrrad", "Mobilität"),
                address = Address("Ludwigsburger Straße 11", "Backnang", "71522"),
                //format is lng-lat
                location = listOf(9.430380, 48.923069),
                rentableDateRange = DateRangeDuration(
                    LocalDate.parse("2021-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    LocalDate.parse("2025-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                ),
                rents = mutableListOf(
                    Rent(
                        "214234235",
                        DateRangeDuration(
                            LocalDate.parse("2021-11-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            LocalDate.parse("2021-12-15", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        ),
                        "12432342353425345213"
                    ),
                    Rent(
                        "5324234",
                        DateRangeDuration(
                            LocalDate.parse("2021-10-10", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            LocalDate.parse("2021-10-20", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        ),
                        "234210230575"
                    )
                )
            )
        )

        `when`(
            productRepository.findByIdIn(
                listOf("12345", "5678"),
                PageRequest.of(0, 10)
            )
        ).thenReturn(RestResponsePage<ProductModel>(productList))

        //test
        productService.findManyById(listOf("12345", "5678"), PageRequest.of(0, 10))

        verify(productRepository, times(1)).findByIdIn(listOf("12345", "5678"), PageRequest.of(0, 10))
    }

    @Test
    fun `should accept rent request and return updated rent request`() {
        val rentRequestInDBMocked = RentRequestModel(
            id = "2222",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "3333"
        )

        val rentRequestInDBMockedAfterAccept = RentRequestModel(
            id = "2222",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "3333",
            status = "accepted"
        )

        val productInDb = ProductModel(
            id = "3333",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "5678",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            location = listOf(9.1938525, 48.8848654),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        whenever(rentRequestService.findById("2222")).thenReturn(rentRequestInDBMocked)
        whenever(productRepository.findById("3333")).thenReturn(Optional.of(productInDb))
        whenever(productRepository.save(any<ProductModel>())).doAnswer { it.arguments[0] as ProductModel }
        whenever(rentRequestService.updateStatus("accepted", rentRequestInDBMocked)).thenReturn(
            rentRequestInDBMockedAfterAccept
        )

        //test
        productService.acceptOrRejectRentRequest("3333", "2222", true, "5678")

        verify(rentRequestService, times(1)).findById("2222")
        verify(productRepository, times(1)).findById("3333")
        verify(elasticProductService, times(1)).save(any<ElasticProductModel>())
        verify(rentRequestService, times(1)).updateStatus("accepted", rentRequestInDBMocked)
    }

    @Test
    fun `should throw RentRequestNotFoundException when trying to accept non-existing rent request`() {
        val productInDb = ProductModel(
            id = "3333",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "5678",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            location = listOf(9.1938525, 48.8848654),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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
        whenever(productRepository.findById("3333")).thenReturn(Optional.of(productInDb))
        whenever(rentRequestService.findById("124223532523")).doThrow(RentRequestNotFoundException("124223532523"))

        assertThrows(RentRequestNotFoundException::class.java) {
            productService.acceptOrRejectRentRequest(
                "3333",
                "124223532523",
                true,
                "1234"
            )
        }
    }

    @Test
    fun `should throw ProductNotFoundException when trying to accept rent request for non existing product`() {
        val rentRequestInDBMocked = RentRequestModel(
            id = "2222",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "3333"
        )

        ProductModel(
            id = "3333",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "5678",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            location = listOf(9.1938525, 48.8848654),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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
        whenever(rentRequestService.findById("2222")).thenReturn(rentRequestInDBMocked)
        whenever(productRepository.findById("235235456345")).doThrow(ProductNotFoundException("235235456345"))

        assertThrows(ProductNotFoundException::class.java) {
            productService.acceptOrRejectRentRequest(
                "235235456345",
                "2222",
                true,
                "1234"
            )
        }
    }

    @Test
    fun `should throw NotAllowedAccessToResourceException when trying to accept rent request of product owned by other user`() {
        val rentRequestInDBMocked = RentRequestModel(
            id = "2222",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "3333"
        )

        val rentRequestInDBMockedAfterAccept = RentRequestModel(
            id = "2222",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "3333",
            status = "accepted"
        )

        val productInDb = ProductModel(
            id = "3333",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "4567",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            location = listOf(9.1938525, 48.8848654),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        whenever(rentRequestService.findById("2222")).thenReturn(rentRequestInDBMocked)
        whenever(productRepository.findById("3333")).thenReturn(Optional.of(productInDb))
        whenever(productRepository.save(any<ProductModel>())).doAnswer { it.arguments[0] as ProductModel }
        whenever(rentRequestService.updateStatus("accepted", rentRequestInDBMocked)).thenReturn(
            rentRequestInDBMockedAfterAccept
        )

        assertThrows(NotAllowedAccessToResourceException::class.java) {
            productService.acceptOrRejectRentRequest(
                "3333",
                "2222",
                true,
                "1234"
            )
        }
    }

    @Test
    fun `should get products with corresponding rent requests`() {
        val rentRequest0ForProduct0 = RentRequestModel(
            id = "1111",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "0"
        )

        val rentRequest1ForProduct0 = RentRequestModel(
            id = "2222",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "0"
        )

        val product0 = ProductModel(
            id = "0",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "1337",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            location = listOf(9.1938525, 48.8848654),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        val rentRequest0ForProduct1 = RentRequestModel(
            id = "3333",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "1"
        )

        val product1 = ProductModel(
            id = "1",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "1337",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            location = listOf(9.1938525, 48.8848654),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        whenever(productRepository.findByOwnerOfProductUserId("1337")).thenReturn(listOf(product0, product1))
        whenever(rentRequestRepository.findByRentRequestReceiverUserId("1337")).thenReturn(
            listOf(
                rentRequest0ForProduct0,
                rentRequest1ForProduct0,
                rentRequest0ForProduct1
            )
        )
        //test
        val productsWithRentRequests = productService.getProductsWithRentRequestsForUser("1337")
        assertEquals(2, productsWithRentRequests.size)
        assertEquals(2, productsWithRentRequests[0].rentRequests.size)
        assertEquals(1, productsWithRentRequests[1].rentRequests.size)
    }

    @Test
    fun `should get products with corresponding rent requests (but no rent request existing yet) for user`() {
        val product0 = ProductModel(
            id = "0",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "1337",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            location = listOf(9.1938525, 48.8848654),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        val product1 = ProductModel(
            id = "1",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "1337",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            location = listOf(9.1938525, 48.8848654),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        whenever(productRepository.findByOwnerOfProductUserId("1337")).thenReturn(listOf(product0, product1))

        //test
        val productsWithRentRequests = productService.getProductsWithRentRequestsForUser("1337")
        assertEquals(2, productsWithRentRequests.size)
        assertEquals(0, productsWithRentRequests[0].rentRequests.size)
        assertEquals(0, productsWithRentRequests[1].rentRequests.size)
    }

    @Test
    fun `should add rent to product and save product and return it`() {
        val rentRequestToBeAddedAsRent = RentRequestModel(
            id = "2222",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "3333"
        )

        val productToAddRentTo = ProductModel(
            id = "3333",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "5678",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            location = listOf(9.1938525, 48.8848654),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        whenever(productRepository.save(any<ProductModel>())).doAnswer { it.arguments[0] as ProductModel }

        //test
        val productReturnedByService = productService.addRentToProduct(productToAddRentTo, rentRequestToBeAddedAsRent)

        assertNotNull(productReturnedByService.rents?.find { it.rentId == rentRequestToBeAddedAsRent.id })
        assertEquals(
            productReturnedByService.rents?.find { it.rentId == rentRequestToBeAddedAsRent.id }?.rentDuration?.fromDate,
            LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        )
        assertEquals(
            productReturnedByService.rents?.find { it.rentId == rentRequestToBeAddedAsRent.id }?.rentDuration?.toDate,
            LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        )
    }

    @Test
    fun `should return rent requests started by user`() {
        val rentRequestByUser0 = RentRequestModel(
            id = "1111",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "0"
        )

        val requestedProduct0 = ProductModel(
            id = "0",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "5678",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            location = listOf(9.1938525, 48.8848654),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        val rentRequestByUser1 = RentRequestModel(
            id = "2222",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "1"
        )

        val requestedProduct1 = ProductModel(
            id = "1",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "5678",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            location = listOf(9.1938525, 48.8848654),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
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

        whenever(rentRequestRepository.findByRequesterUserId("1234")).thenReturn(
            listOf(
                rentRequestByUser0,
                rentRequestByUser1
            )
        )
        whenever(productRepository.findByIdIn(listOf("0", "1"))).thenReturn(
            listOf(
                requestedProduct0,
                requestedProduct1
            )
        )

        //test
        val productReturnedByService = productService.getRentRequestsWithProducts("1234")

        assertEquals(2, productReturnedByService.size)
        assertEquals("0", productReturnedByService[0].product.id)
        assertEquals("1", productReturnedByService[1].product.id)
        assertEquals("1111", productReturnedByService[0].rentRequest.id)
        assertEquals("2222", productReturnedByService[1].rentRequest.id)
    }

    @Test
    fun `should return empty list when trying to access rent requests by new user`() {
        //test
        val productReturnedByService = productService.getRentRequestsWithProducts("1234")
        assertEquals(0, productReturnedByService.size)
    }

}