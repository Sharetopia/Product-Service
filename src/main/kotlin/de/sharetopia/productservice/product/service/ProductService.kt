package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.dto.ProductDTO
import de.sharetopia.productservice.product.model.ProductModel
import de.sharetopia.productservice.product.model.RentRequestModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

interface ProductService {
  fun findAll(): List<ProductModel>

  fun create(product: ProductModel): ProductModel

  fun updateOrInsert(productId: String, product: ProductModel): ProductModel

  fun partialUpdate(productId: String, product: ProductModel, updatedFieldsProductDTO: ProductDTO): ProductModel

  fun findById(productId: String): Optional<ProductModel>

  fun deleteById(productId: String)

  fun findManyById(ids: List<String>, pageable: Pageable): Page<ProductModel>

  fun addRentToProduct(product: ProductModel, rentRequest: RentRequestModel): ProductModel
}
