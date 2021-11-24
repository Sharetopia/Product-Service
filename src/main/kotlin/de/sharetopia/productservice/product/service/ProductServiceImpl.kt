package de.sharetopia.productservice.product

import de.sharetopia.productservice.ProductModel
import de.sharetopia.productservice.ProductRepository
import java.util.Optional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProductServiceImpl : ProductService {
  @Autowired private lateinit var productRepository: ProductRepository

  override fun findAll(): List<ProductModel> = productRepository.findAll()

  override fun saveOrUpdate(product: ProductModel): ProductModel {
    return productRepository.save(product)
  }

  override fun findById(productId: String): Optional<ProductModel> {
    return productRepository.findById(productId)
  }

  override fun deleteById(productId: String) {
    productRepository.deleteById(productId)
  }
}
