package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.exception.ProductNotFoundException
import de.sharetopia.productservice.product.model.RentRequestModel
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.repository.RentRequestRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class RentRequestServiceImpl : RentRequestService {
    @Autowired
    private lateinit var rentRequestRepository: RentRequestRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    private val log: Logger = LoggerFactory.getLogger(RentRequestServiceImpl::class.java)

    override fun findAll(): List<RentRequestModel> = rentRequestRepository.findAll()

    override fun create(rentRequest: RentRequestModel, userId: String): RentRequestModel {
        rentRequest.requesterUserId = userId
        val requestedProduct = productRepository.findById(rentRequest.requestedProductId!!).orElseThrow {
            log.error("Error fetching requested product by id. {error=ProductNotFoundException, method=POST, endpoint=/products, requesterUserId=${userId}}")
            ProductNotFoundException(rentRequest.requestedProductId!!)
        }
        rentRequest.rentRequestReceiverUserId = requestedProduct.ownerOfProductUserId
        return rentRequestRepository.save(rentRequest)
    }

    override fun updateStatus(newStatus: String, rentRequest: RentRequestModel): RentRequestModel {
        rentRequest.status = newStatus
        return rentRequestRepository.save(rentRequest)
    }

    override fun findById(rentRequestId: String): Optional<RentRequestModel> {
        return rentRequestRepository.findById(rentRequestId)
    }

    override fun deleteById(rentRequestId: String) {
        rentRequestRepository.deleteById(rentRequestId)
    }

}