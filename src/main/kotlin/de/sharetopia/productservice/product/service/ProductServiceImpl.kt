package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.dto.ProductDTO
import de.sharetopia.productservice.product.model.ProductModel
import de.sharetopia.productservice.product.repository.ProductRepository
import org.bson.types.ObjectId
import java.util.Optional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProductServiceImpl : ProductService {
  @Autowired private lateinit var productRepository: ProductRepository

  override fun findAll(): List<ProductModel> = productRepository.findAll()

  override fun create(product: ProductModel): ProductModel {
    return productRepository.insert(product)
  }

  override fun updateOrInsert(productId: String, product: ProductModel): ProductModel{
    product.id = ObjectId(productId) //TODO
    return productRepository.save(product)
  }

  override fun partialUpdate(productId: String, product: ProductModel): ProductModel {
    product.id = ObjectId(productId)
    return productRepository.save(product);
  }
  override fun findById(productId: String): Optional<ProductModel> {
    return productRepository.findById(productId)
  }

  override fun deleteById(productId: String) {
    productRepository.deleteById(productId)
  }
}
