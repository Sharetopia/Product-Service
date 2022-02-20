package de.sharetopia.productservice.unitTests

import de.sharetopia.productservice.product.exception.ProductNotFoundException
import de.sharetopia.productservice.product.model.Address
import de.sharetopia.productservice.product.model.DateRangeDuration
import de.sharetopia.productservice.product.model.ProductModel
import de.sharetopia.productservice.product.model.RentRequestModel
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.repository.RentRequestRepository
import de.sharetopia.productservice.product.service.ProductService
import de.sharetopia.productservice.product.service.RentRequestService
import de.sharetopia.productservice.product.service.RentRequestServiceImpl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RentRequestServiceTest {
    @Mock
    lateinit var rentRequestRepository: RentRequestRepository

    @Mock
    lateinit var productService: ProductService

    @InjectMocks
    var rentRequestService: RentRequestService = RentRequestServiceImpl()

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `should find all rent requests`() {
        val rentRequestList = listOf(
            RentRequestModel(
                id = "1111",
                fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                requesterUserId = "1234",
                rentRequestReceiverUserId = "5678",
                requestedProductId = "747"
            ),
            RentRequestModel(
                id = "2222",
                fromDate = LocalDate.parse("2022-06-15", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                toDate = LocalDate.parse("2022-06-21", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                requesterUserId = "5678",
                rentRequestReceiverUserId = "1234",
                requestedProductId = "747"
            )
        )

        whenever(rentRequestRepository.findAll()).thenReturn(rentRequestList)

        //test
        val rentRequestListReturnedByService = rentRequestService.findAll()

        Assertions.assertEquals(2, rentRequestListReturnedByService.size)
        Assertions.assertEquals("1111", rentRequestListReturnedByService[0].id)
        Assertions.assertEquals("2222", rentRequestListReturnedByService[1].id)
        verify(rentRequestRepository, times(1)).findAll()
    }

    @Test
    fun `should save rent request`() {
        val rentRequestToCreate = RentRequestModel(
            id = "1111",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "747"
        )

        val productMockedInDb = ProductModel(
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
            )
        )

        whenever(rentRequestRepository.save(any<RentRequestModel>())).thenReturn(rentRequestToCreate)
        whenever(productService.findById(any<String>())).thenReturn(productMockedInDb)

        //test
        rentRequestService.create(rentRequestToCreate, "1234")

        verify(rentRequestRepository, times(1)).save(argThat {
            (id === "1111") &&
                    (fromDate === rentRequestToCreate.fromDate) &&
                    (toDate === rentRequestToCreate.toDate) &&
                    (requesterUserId === rentRequestToCreate.requesterUserId) &&
                    (rentRequestReceiverUserId === rentRequestToCreate.rentRequestReceiverUserId) &&
                    (requestedProductId === rentRequestToCreate.requestedProductId)
        })
    }

    @Test
    fun `should throw ProductNotFoundException when trying to create rent request for non-existing product`() {
        val rentRequestToCreate = RentRequestModel(
            id = "1111",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "747"
        )

        ProductModel(
            id = "12345",
            title = "Rennrad Rot",
            description = "Das ist mein rotes Rennrad",
            ownerOfProductUserId = "5678",
            tags = listOf("Fahrrad", "Mobilität"),
            price = BigDecimal(12.99),
            address = Address("Nobelstraße 10", "Stuttgart", "70569"),
            location = listOf(9.100591, 48.7419328),
            rentableDateRange = DateRangeDuration(
                LocalDate.parse("2021-10-10", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                LocalDate.parse("2022-04-10", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            )
        )

        whenever(rentRequestRepository.save(any<RentRequestModel>())).thenReturn(rentRequestToCreate)

        //test
        assertThrows(ProductNotFoundException::class.java) {
            rentRequestService.create(
                rentRequestToCreate,
                "1234"
            )
        }
    }

    @Test
    fun `should update status of rent request`() {
        val rentRequestToUpdate = RentRequestModel(
            id = "1111",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "747"
        )

        whenever(rentRequestRepository.save(any<RentRequestModel>())).doAnswer { it.arguments[0] as RentRequestModel }

        //test
        rentRequestService.updateStatus("accepted", rentRequestToUpdate)

        verify(rentRequestRepository, times(1)).save(argThat {
            (status === "accepted")
        })
    }

    @Test
    fun `should find rent request by id`() {
        val rentRequestMockedInDb = RentRequestModel(
            id = "1111",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "747"
        )
        whenever(rentRequestRepository.findById(any<String>())).thenReturn(Optional.of(rentRequestMockedInDb))

        //test
        rentRequestService.findById("1111")
        verify(rentRequestRepository, times(1)).findById("1111")
    }

    @Test
    fun `should delete rent request by id`() {
        RentRequestModel(
            id = "1111",
            fromDate = LocalDate.parse("2021-12-20", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            toDate = LocalDate.parse("2021-12-28", DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            requesterUserId = "1234",
            rentRequestReceiverUserId = "5678",
            requestedProductId = "747"
        )
        whenever(rentRequestRepository.deleteById(any<String>())).doAnswer { }

        //test
        rentRequestRepository.deleteById("1111")
        verify(rentRequestRepository, times(1)).deleteById("1111")
    }
}