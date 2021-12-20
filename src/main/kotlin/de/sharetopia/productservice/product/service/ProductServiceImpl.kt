package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.model.ProductModel
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.util.GeoCoder
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*


@Service
class ProductServiceImpl : ProductService {
  @Autowired private lateinit var productRepository: ProductRepository

  override fun findAll(): List<ProductModel> = productRepository.findAll()

  override fun create(product: ProductModel): ProductModel {
    product.location= GeoCoder.getCoordinatesForAddress(product.address)
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

  override fun findManyById(ids: List<String>, pageable: Pageable): Page<ProductModel> {
    return productRepository.findByIdIn(ids, pageable)
  }
}
