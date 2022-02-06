package de.sharetopia.productservice.product.repository

import de.sharetopia.productservice.product.model.ProductModel
import de.sharetopia.productservice.product.model.RentRequestModel
import org.springframework.data.geo.Distance
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.data.mongodb.repository.MongoRepository

public interface RentRequestRepository : MongoRepository<RentRequestModel, String> {
    fun findByRequestedProductId(requestedProductId: String): List<RentRequestModel>
    fun findByRentRequestReceiverUserId(rentRequestReceiverUserId: String): List <RentRequestModel>
    fun findByRequesterUserId(requesterUserId: String): List <RentRequestModel>
}