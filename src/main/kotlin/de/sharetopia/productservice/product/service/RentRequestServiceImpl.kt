package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.exception.ProductNotFoundException
import de.sharetopia.productservice.product.model.RentRequestModel
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.repository.RentRequestRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class RentRequestServiceImpl: RentRequestService {
    @Autowired
    private lateinit var rentRequestRepository: RentRequestRepository
    @Autowired
    private lateinit var productRepository: ProductRepository

    override fun findAll(): List<RentRequestModel> = rentRequestRepository.findAll()

    override fun create(rentRequest: RentRequestModel, userId: String): RentRequestModel {
        rentRequest.requesterUserId = userId
        val requestedProduct = productRepository.findById(rentRequest.requestedProductId!!).orElseThrow { ProductNotFoundException(rentRequest.requestedProductId!!) }
        rentRequest.rentRequestReceiverUserId = requestedProduct.ownerOfProductUserId
        return rentRequestRepository.insert(rentRequest)
    }

    override fun updateStatus(newStatus: String, rentRequest: RentRequestModel): RentRequestModel{
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