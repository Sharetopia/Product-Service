package de.sharetopia.productservice.unitTests

import RestResponsePage
import de.sharetopia.productservice.product.model.*
import de.sharetopia.productservice.product.repository.ElasticProductRepository
import de.sharetopia.productservice.product.service.ElasticProductService
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ElasticProductServiceTest {
    @Mock
    lateinit var elasticProductRepository: ElasticProductRepository

    @InjectMocks
    lateinit var elasticProductService: ElasticProductService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `should save and return product`() {
        val product = ProductModel(
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
        )
        val elasticProduct = ObjectMapperUtils.map(product, ElasticProductModel::class.java)
        whenever(elasticProductRepository.save(any<ElasticProductModel>())).thenReturn(elasticProduct)

        //test
        elasticProductService.save(elasticProduct)

        verify(elasticProductRepository, times(1)).save(elasticProduct)
    }

    @Test
    fun `should find by title and near coordinates`() {
        val product = ProductModel(
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
        )


        val elasticProduct = ObjectMapperUtils.map(product, ElasticProductModel::class.java)
        whenever(
            elasticProductRepository.findByTitleAndNear(
                any<String>(),
                any<Int>(),
                any<Double>(),
                any<Double>(),
                any<Pageable>()
            )
        ).thenReturn(
            RestResponsePage<ElasticProductModel>(
                listOf(elasticProduct)
            )
        )

        //test
        elasticProductService.findByTitleAndNearCoordinates("Rennrad", 10, 3.4366, 3.4366, PageRequest.of(0, 10))

        verify(elasticProductRepository, times(1)).findByTitleAndNear(
            "Rennrad",
            10,
            3.4366,
            3.4366,
            PageRequest.of(0, 10)
        )
    }

    @Test
    fun `should find by title and near city without date`() {
        val product = ProductModel(
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
        )


        val elasticProduct = ObjectMapperUtils.map(product, ElasticProductModel::class.java)
        whenever(
            elasticProductRepository.findByTitleAndNear(
                any<String>(),
                any<Int>(),
                any<Double>(),
                any<Double>(),
                any<Pageable>()
            )
        ).thenReturn(
            RestResponsePage<ElasticProductModel>(
                listOf(elasticProduct)
            )
        )

        //test
        elasticProductService.findByTitleAndNearCityWithOptionalDate(
            "Rennrad",
            10,
            "70569",
            null,
            null,
            PageRequest.of(0, 10),
        )
        verify(elasticProductRepository, times(1)).findByTitleAndNear(
            "Rennrad",
            10,
            48.74324166631358,
            9.111422853155403,
            PageRequest.of(0, 10)
        )
    }

    @Test
    fun `should find by title and near city with date`() {
        val product = ProductModel(
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
        )

        val elasticProduct = ObjectMapperUtils.map(product, ElasticProductModel::class.java)
        whenever(
            elasticProductRepository.findByTitleOrTagsAndAvailabilityAndNear(
                any<String>(),
                any<Int>(),
                any<Double>(),
                any<Double>(),
                any<LocalDate>(),
                any<LocalDate>(),
                any<Pageable>()
            )
        ).thenReturn(
            RestResponsePage<ElasticProductModel>(
                listOf(elasticProduct)
            )
        )

        //test
        elasticProductService.findByTitleAndNearCityWithOptionalDate(
            "Rennrad",
            10,
            "70569",
            LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            LocalDate.parse("2021-12-23", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            PageRequest.of(0, 10)
        )
        verify(elasticProductRepository, times(1)).findByTitleOrTagsAndAvailabilityAndNear(
            "Rennrad",
            10,
            48.74324166631358,
            9.111422853155403,
            LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            LocalDate.parse("2021-12-23", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            PageRequest.of(0, 10)
        )
    }

    @Test
    fun `should delete product by id`() {
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
        )

        whenever(elasticProductRepository.deleteById(any<String>())).doAnswer { }

        //test
        elasticProductService.deleteById("12345")
        verify(elasticProductRepository, times(1)).deleteById("12345")
    }
}