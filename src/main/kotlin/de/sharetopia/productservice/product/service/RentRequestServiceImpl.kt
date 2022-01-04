package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.model.RentRequestModel
import de.sharetopia.productservice.product.repository.RentRequestRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class RentRequestServiceImpl: RentRequestService {
    @Autowired
    private lateinit var rentRequestRepository: RentRequestRepository

    override fun findAll(): List<RentRequestModel> = rentRequestRepository.findAll()

    override fun create(rentRequest: RentRequestModel): RentRequestModel {
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