package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.model.ProductModel
import java.util.Optional

interface ProductService {
  fun findAll(): List<ProductModel>

  fun create(product: ProductModel): ProductModel

  fun updateOrInsert(productId: String, product: ProductModel): ProductModel

  fun partialUpdate(productId: String, product: ProductModel): ProductModel

  fun findById(productId: String): Optional<ProductModel>

  fun deleteById(productId: String)
}
