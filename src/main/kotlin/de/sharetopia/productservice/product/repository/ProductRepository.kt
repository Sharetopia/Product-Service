package de.sharetopia.productservice

import org.springframework.data.mongodb.repository.MongoRepository

public interface ProductRepository : MongoRepository<ProductModel, String>
