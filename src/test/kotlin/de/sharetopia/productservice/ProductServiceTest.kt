package de.sharetopia.productservice

import RestResponsePage
import de.sharetopia.productservice.product.dto.ProductDTO
import de.sharetopia.productservice.product.model.*
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.repository.RentRequestRepository
import de.sharetopia.productservice.product.service.ElasticProductService
import de.sharetopia.productservice.product.service.ProductService
import de.sharetopia.productservice.product.service.ProductServiceImpl
import de.sharetopia.productservice.product.service.RentRequestService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.verification.After
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import org.mockito.kotlin.any


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
    private lateinit var rentRequestRepository: RentRequestRepository

    @InjectMocks
    var productService: ProductService = ProductServiceImpl()

    @BeforeEach
    fun setup(){
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
        val productToCreate =
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

        `when`(productRepository.insert(productToCreate)).thenReturn(productToCreate)

        //test
        val productReturnedByService =
            productService.create(productToCreate, userId = "204e1304-26f0-47b5-b353-cee12f4c8d34")



        assertEquals(productToCreate.id, productReturnedByService.id)
        assertEquals(productToCreate.title, productReturnedByService.title)
        assertEquals(productToCreate.description, productReturnedByService.description)
        assertEquals(productToCreate.ownerOfProductUserId, productReturnedByService.ownerOfProductUserId)
        assertEquals(productToCreate.tags, productReturnedByService.tags)
        assertEquals(productToCreate.price, productReturnedByService.price)
        assertThat(productToCreate.rentableDateRange).usingRecursiveComparison()
            .isEqualTo(productReturnedByService.rentableDateRange)
        assertThat(productToCreate.rents).usingRecursiveComparison().isEqualTo(productReturnedByService.rents)
        assertThat((9.100590 < productReturnedByService.location?.get(0)!!) && (productReturnedByService.location?.get(0)!! < 9.100592))
        assertThat(
            (48.7419327 < productReturnedByService.location?.get(0)!!) && (productReturnedByService.location?.get(
                0
            )!! < 48.7419329)
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
        `when`(productRepository.findById(anyString())).thenReturn(Optional.of(mockedProductInDb))
        `when`(productRepository.save(updateProduct)).thenReturn(updateProduct)

        //test
        val productReturnedByService =
            productService.updateOrInsert("12345", updateProduct, userId = "204e1304-26f0-47b5-b353-cee12f4c8d34")

        verify(elasticProductService, times(1)).save(any())
        assertEquals(updateProduct.title, productReturnedByService.title)
        assertEquals(updateProduct.description, productReturnedByService.description)
        assertThat(updateProduct.address).usingRecursiveComparison().isEqualTo(productReturnedByService.address)
        assertThat((9.430379 < productReturnedByService.location?.get(0)!!) && (productReturnedByService.location?.get(0)!! < 9.430381))
        assertThat(
            (48.923068 < productReturnedByService.location?.get(0)!!) && (productReturnedByService.location?.get(
                0
            )!! < 48.923070)
        )
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
                description = "Das ist mein blaues Rennrad"
            )

        val updatedModel = ProductModel(
            id = "12345",
            title = "Rennrad Blau",
            description = "Das ist mein blaues Rennrad",
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
        `when`(productRepository.save(any(ProductModel::class.java))).thenReturn(updatedModel)


        //test
        val productReturnedByService = productService.partialUpdate("12345", mockedProductInDb, updateFieldsProduct)

        verify(productRepository).save(argThat { productModel: ProductModel ->
            (productModel.id === "12345") &&
            (productModel.title === updateFieldsProduct.title)
        })

        verify(productRepository, times(1)).save(any(ProductModel::class.java))

        assertEquals(updateFieldsProduct.title, productReturnedByService.title)
        assertEquals(updateFieldsProduct.description, productReturnedByService.description)
        assertEquals(mockedProductInDb.price, productReturnedByService.price)
        assertEquals(mockedProductInDb.tags, productReturnedByService.tags)
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
        val productReturnedByService = productService.findById("12345").get()
        verify(productRepository, times(1)).findById(productInDb.id)

        assertEquals(productInDb.id, productReturnedByService.id)
        assertEquals(productInDb.title, productReturnedByService.title)
        assertEquals(productInDb.description, productReturnedByService.description)
        assertEquals(productInDb.ownerOfProductUserId, productReturnedByService.ownerOfProductUserId)
        assertEquals(productInDb.tags, productReturnedByService.tags)
        assertEquals(productInDb.price, productReturnedByService.price)
        assertThat(productInDb.rentableDateRange).usingRecursiveComparison()
            .isEqualTo(productReturnedByService.rentableDateRange)
        assertThat(productInDb.rents).usingRecursiveComparison().isEqualTo(productReturnedByService.rents)
        assertEquals(productInDb.location, productReturnedByService.location)

    }

    @Test
    fun `should delete product by id`() {
        val idOfProductToDelete = "12345"
        productService.deleteById(idOfProductToDelete)
        verify(productRepository, times(1)).deleteById(idOfProductToDelete)
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
        val productListReturnedByService = productService.findManyById(listOf("12345", "5678"), PageRequest.of(0, 10))

        assertEquals(2, productListReturnedByService.size)
        assertEquals("12345", productListReturnedByService.content[0].id)
        assertEquals("5678", productListReturnedByService.content[1].id)
        verify(productRepository, times(1)).findByIdIn(anyList(), any())
    }

    @Test
    fun `should accept rent request and return updated rent request`() {

    }


}