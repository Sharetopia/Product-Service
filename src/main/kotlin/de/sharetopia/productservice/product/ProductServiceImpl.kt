package de.sharetopia.productservice.product

import de.sharetopia.productservice.Product
import de.sharetopia.productservice.ProductRepository
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*


@Service
class ProductServiceImpl : ProductService {
    @Autowired
    private lateinit var productRepository: ProductRepository

    override fun findAll(): List<Product> = productRepository.findAll()

    override fun saveOrUpdateProduct(product: Product): Product {
        return productRepository.save(product);
    }

    override fun findProductById(productId: String): Optional<Product> {
        return productRepository.findById(productId)
    }

    override fun deleteProductById(productId: String) {
        productRepository.deleteById(productId);
    }
}