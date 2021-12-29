package de.sharetopia.productservice

import org.springframework.data.mongodb.repository.MongoRepository

interface ProductRepository : MongoRepository<ProductModel, String>
