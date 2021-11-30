package de.sharetopia.productservice.product.repository

import de.sharetopia.productservice.product.model.ProductModel
import org.springframework.data.mongodb.repository.MongoRepository

public interface ProductRepository : MongoRepository<ProductModel, String>
