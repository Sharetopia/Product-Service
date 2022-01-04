package de.sharetopia.productservice.product.repository

import de.sharetopia.productservice.product.model.RentRequestModel
import org.springframework.data.mongodb.repository.MongoRepository

public interface RentRequestRepository : MongoRepository<RentRequestModel, String> {
}