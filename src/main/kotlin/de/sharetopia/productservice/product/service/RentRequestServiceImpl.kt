package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.exception.NotAllowedAccessToResourceException
import de.sharetopia.productservice.product.exception.RentRequestNotFoundException
import de.sharetopia.productservice.product.model.RentRequestModel
import de.sharetopia.productservice.product.repository.RentRequestRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RentRequestServiceImpl : RentRequestService {
    @Autowired
    private lateinit var rentRequestRepository: RentRequestRepository

    @Autowired
    private lateinit var productService: ProductService

    private val log: Logger = LoggerFactory.getLogger(RentRequestServiceImpl::class.java)

    override fun findAll(): List<RentRequestModel> = rentRequestRepository.findAll()

    override fun create(rentRequest: RentRequestModel, userId: String): RentRequestModel {
        rentRequest.requesterUserId = userId
        val requestedProduct = productService.findById(rentRequest.requestedProductId!!)
        rentRequest.rentRequestReceiverUserId = requestedProduct.ownerOfProductUserId
        return rentRequestRepository.save(rentRequest)
    }

    override fun updateStatus(newStatus: String, rentRequest: RentRequestModel): RentRequestModel {
        rentRequest.status = newStatus
        return rentRequestRepository.save(rentRequest)
    }

    override fun findById(rentRequestId: String): RentRequestModel {
        val rentRequest = rentRequestRepository.findById(rentRequestId).orElseThrow {
            log.error("Error fetching rent request by id. {error=RentRequestNotFoundException, rentRequestId=$rentRequestId")
            RentRequestNotFoundException(rentRequestId)
        }
        return rentRequest
    }

    override fun deleteById(rentRequestId: String, userId: String) {
        val rentRequestToBeDeleted = findById(rentRequestId)
        if (rentRequestToBeDeleted.requesterUserId != userId) {
            log.error("Error by not allowed access to rent request. {NotAllowedAccessToResourceException, rentRequestId=$rentRequestId")
            throw NotAllowedAccessToResourceException(userId)
        }
        rentRequestRepository.deleteById(rentRequestId)
    }

}