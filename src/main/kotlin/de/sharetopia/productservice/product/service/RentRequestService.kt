package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.model.RentRequestModel
import java.util.*

interface RentRequestService {
    fun findAll(): List<RentRequestModel>

    fun create(rentRequest: RentRequestModel, userId: String): RentRequestModel

    fun updateStatus(newStatus: String, rentRequest: RentRequestModel): RentRequestModel

    fun findById(rentRequestId: String): Optional<RentRequestModel>

    fun deleteById(rentRequestId: String)

}