package de.sharetopia.productservice.product

import de.sharetopia.productservice.ProductModel
import java.util.Optional

interface ProductService {
  fun findAll(): List<ProductModel>

  fun saveOrUpdate(product: ProductModel): ProductModel

  fun findById(productId: String): Optional<ProductModel>

  fun deleteById(productId: String)
}
