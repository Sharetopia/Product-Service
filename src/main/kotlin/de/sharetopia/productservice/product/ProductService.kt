package de.sharetopia.productservice.product

import de.sharetopia.productservice.Product
import org.bson.types.ObjectId
import java.util.*

interface ProductService {
    fun findAll(): List<Product>

    fun saveOrUpdateProduct(product: Product): Product

    fun findProductById(productId: String): Optional<Product>

    fun deleteProductById(productId: String)

}