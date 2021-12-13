package de.sharetopia.productservice.product.repository

import de.sharetopia.productservice.product.model.ProductModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.geo.Distance
import org.springframework.data.mongodb.core.geo.GeoJsonPoint
import org.springframework.data.mongodb.repository.MongoRepository

public interface ProductRepository : MongoRepository<ProductModel, String>{
    fun findByLocationNear(p: GeoJsonPoint?, d: Distance?): List<ProductModel?>?
    fun findByIdIn(ids: List<String>, pageable: Pageable): Page<ProductModel>
}

