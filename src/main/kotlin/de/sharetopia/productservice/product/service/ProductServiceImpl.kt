package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.dto.ProductDTO
import de.sharetopia.productservice.product.dto.UserProductsWithRentRequestsView
import de.sharetopia.productservice.product.dto.UserSentRentRequestsWithProductsView
import de.sharetopia.productservice.product.model.*
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.repository.RentRequestRepository
import de.sharetopia.productservice.product.util.GeoCoder
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*


@Service
class ProductServiceImpl : ProductService {
  @Autowired private lateinit var productRepository: ProductRepository
  @Autowired private lateinit var elasticProductService: ElasticProductService
  @Autowired private lateinit var rentRequestRepository: RentRequestRepository

  override fun findAll(): List<ProductModel> = productRepository.findAll()

  override fun create(product: ProductModel, userId: String): ProductModel {
    product.location= GeoCoder.getCoordinatesForAddress(product.address)
    product.ownerOfProductUserId = userId

    val createdProductModel = productRepository.insert(product)
    val elasticProductModel = ObjectMapperUtils.map(createdProductModel, ElasticProductModel::class.java)
    elasticProductService.save(elasticProductModel)

    return createdProductModel
  }

  override fun updateOrInsert(productId: String, product: ProductModel, userId: String): ProductModel{
    product.id = productId
    product.location = GeoCoder.getCoordinatesForAddress(product.address)
    product.ownerOfProductUserId = userId

    val updatedProduct = productRepository.save(product)
    val elasticProductModel = ObjectMapperUtils.map(updatedProduct, ElasticProductModel::class.java)
    elasticProductService.save(elasticProductModel)

    return updatedProduct
  }

  override fun partialUpdate(productId: String, storedProductModel: ProductModel, updatedFieldsProductDTO: ProductDTO): ProductModel {
    storedProductModel.id = productId
    if(updatedFieldsProductDTO.address != null){
      updatedFieldsProductDTO.location = GeoCoder.getCoordinatesForAddress(updatedFieldsProductDTO.address!!)
    }
    return productRepository.save(storedProductModel.copy(
      title = updatedFieldsProductDTO.title ?: storedProductModel.title,
      description = updatedFieldsProductDTO.description ?: storedProductModel.description,
      tags = updatedFieldsProductDTO.tags ?: storedProductModel.tags,
      address = updatedFieldsProductDTO.address ?: storedProductModel.address,
      location = updatedFieldsProductDTO.location ?: storedProductModel.location,
      rentableDateRange = updatedFieldsProductDTO.rentableDateRange ?: storedProductModel.rentableDateRange,
      rents = (updatedFieldsProductDTO.rents ?: storedProductModel.rents) as MutableList<Rent>?,
    ))
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

  override fun addRentToProduct(product: ProductModel, rentRequest: RentRequestModel): ProductModel {
    product.rents?.add(
      Rent(
        rentRequest.requesterUserId,
        DateRangeDuration(rentRequest.fromDate, rentRequest.toDate),
        rentRequest.id.toString()
      )
    )
    return productRepository.save(product)
  }

  override fun getProductsWithRentRequestsForUser(userId: String): MutableList<UserProductsWithRentRequestsView>{
    val productsOfferedByUser = productRepository.findByOwnerOfProductUserId(userId)
    val rentRequestsForProductsOfUser = rentRequestRepository.findByRentRequestReceiverUserId(userId)

    val productsWithRentRequests: MutableList<UserProductsWithRentRequestsView> = mutableListOf()
    for (product in productsOfferedByUser) {
      val rentRequestsForProducts = rentRequestsForProductsOfUser.filter { it.requestedProductId == product.id.toString()}
      val currentProductRentView = ObjectMapperUtils.map(product, UserProductsWithRentRequestsView::class.java)
      currentProductRentView.rentRequests = rentRequestsForProducts
      productsWithRentRequests.add(currentProductRentView)
    }
    return productsWithRentRequests
  }

  override fun getRentRequestsWithProducts(userId: String): MutableList<UserSentRentRequestsWithProductsView>{
    val rentRequestsByUser = rentRequestRepository.findByRequesterUserId(userId)
    val rentRequestsIds: List<String> = rentRequestsByUser.map { it.requestedProductId!! }
    var requestedProducts = productRepository.findByIdIn(rentRequestsIds)

    val rentRequestsWithProduct: MutableList<UserSentRentRequestsWithProductsView> = mutableListOf()
    for (rentRequest in rentRequestsByUser) {
      val productOfRentRequest = requestedProducts.first { it.id.toString() == rentRequest.requestedProductId}
      rentRequestsWithProduct.add(UserSentRentRequestsWithProductsView(rentRequest, productOfRentRequest))
    }
    return rentRequestsWithProduct
  }
}
