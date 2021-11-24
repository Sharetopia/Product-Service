package de.sharetopia.productservice

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

public interface ProductRepository : MongoRepository<Product, String>
