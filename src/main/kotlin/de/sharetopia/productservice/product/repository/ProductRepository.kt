package de.sharetopia.productservice.product.repository

import de.sharetopia.productservice.product.model.ProductModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository

interface ProductRepository : MongoRepository<ProductModel, String>{
    fun findByIdIn(ids: List<String>, pageable: Pageable): Page<ProductModel>
    fun findByIdIn(ids: List<String>): List<ProductModel>
    fun findByOwnerOfProductUserId(ownerOfProductUserId: String): List <ProductModel>
}

