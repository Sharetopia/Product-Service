package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.dto.*
import de.sharetopia.productservice.product.exception.NotAllowedAccessToResourceException
import de.sharetopia.productservice.product.exception.ProductNotFoundException
import de.sharetopia.productservice.product.exception.productIdUrlBodyMismatchException
import de.sharetopia.productservice.product.model.*
import de.sharetopia.productservice.product.repository.ProductRepository
import de.sharetopia.productservice.product.repository.RentRequestRepository
import de.sharetopia.productservice.product.util.GeoCoder
import de.sharetopia.productservice.product.util.ObjectMapperUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.BigDecimal


@Service
class ProductServiceImpl : ProductService {
    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var elasticProductService: ElasticProductService

    @Autowired
    private lateinit var rentRequestService: RentRequestService

    @Autowired
    private lateinit var rentRequestRepository: RentRequestRepository

    private val log: Logger = LoggerFactory.getLogger(ProductServiceImpl::class.java)

    override fun findAll(): List<ProductModel> = productRepository.findAll()

    override fun create(product: ProductModel, userId: String): ProductModel {
        product.location = GeoCoder.getCoordinatesForAddress(product.address)
        product.ownerOfProductUserId = userId

        val createdProductModel = productRepository.insert(product)
        val elasticProductModel = ObjectMapperUtils.map(createdProductModel, ElasticProductModel::class.java)
        elasticProductService.save(elasticProductModel)

        return createdProductModel
    }

    override fun updateOrInsert(productId: String, product: ProductModel, userId: String): ProductModel {
        val existingProduct = productRepository.findById(productId)
        if (existingProduct.isPresent) {
            if (existingProduct.get().ownerOfProductUserId != userId) {
                log.error("Error trying to update product. {error=NotAllowedAccessToResourceException, method=PUT, endpoint=/products/{id}, requesterUserId=${userId}}")
                throw NotAllowedAccessToResourceException(userId)
            }
        }

        product.id = productId
        product.location = GeoCoder.getCoordinatesForAddress(product.address)
        product.ownerOfProductUserId = userId

        val updatedProduct = productRepository.save(product)
        val elasticProductModel = ObjectMapperUtils.map(updatedProduct, ElasticProductModel::class.java)
        elasticProductService.save(elasticProductModel)

        return updatedProduct
    }

    override fun partialUpdate(
        productId: String,
        updatedFieldsProductDTO: ProductDTO,
        userId: String
    ): ProductModel {
        val storedProductModel = findById(productId)
        storedProductModel.id = productId
        if (storedProductModel.ownerOfProductUserId != userId) {
            log.error("Error by not allowed access to product. {NotAllowedAccessToResourceException, productId=$productId, requesterUserId=${userId}}")
            throw NotAllowedAccessToResourceException(userId)
        }
        if (updatedFieldsProductDTO.address != null) {
            updatedFieldsProductDTO.location = GeoCoder.getCoordinatesForAddress(updatedFieldsProductDTO.address!!)
        }
        val updatedModel = storedProductModel.copy(
            title = updatedFieldsProductDTO.title ?: storedProductModel.title,
            description = updatedFieldsProductDTO.description ?: storedProductModel.description,
            price = if (updatedFieldsProductDTO.price != BigDecimal.ZERO) updatedFieldsProductDTO.price else storedProductModel.price,
            tags = updatedFieldsProductDTO.tags ?: storedProductModel.tags,
            address = updatedFieldsProductDTO.address ?: storedProductModel.address,
            location = updatedFieldsProductDTO.location ?: storedProductModel.location,
            rentableDateRange = updatedFieldsProductDTO.rentableDateRange ?: storedProductModel.rentableDateRange,
            rents = (updatedFieldsProductDTO.rents ?: storedProductModel.rents) as MutableList<Rent>?
        )
        elasticProductService.save(ObjectMapperUtils.map(updatedModel, ElasticProductModel::class.java))
        return productRepository.save(updatedModel)
    }

    override fun findById(productId: String): ProductModel {
        return productRepository.findById(productId).orElseThrow {
            log.error("Error fetching product by id. {error=ProductNotFoundException, productId=$productId}")
            ProductNotFoundException(productId)
        }
    }

    override fun deleteById(productId: String, userId: String) {
        val productToBeDeleted = findById(productId)
        if (productToBeDeleted.ownerOfProductUserId != userId) {
            log.error("Error by not allowed access to product. {NotAllowedAccessToResourceException, productId=$productId, requesterUserId=${userId}}")
            throw NotAllowedAccessToResourceException(userId)
        }
        productRepository.deleteById(productId)
        elasticProductService.deleteById(productId)
    }

    override fun findManyById(ids: List<String>, pageable: Pageable): Page<ProductModel> {
        return productRepository.findByIdIn(ids, pageable)

    }

    override fun acceptOrRejectRentRequest(
        productId: String,
        rentRequestId: String,
        isAccepted: Boolean,
        userId: String
    ): RentRequestModel {
        var rentRequest = rentRequestService.findById(rentRequestId)
        var product = findById(productId)

        if (rentRequest.requestedProductId != productId) {
            log.error("Error trying to accept/reject rent request because of id mismatch. {error=productIdUrlBodyMismatchException, isAccepted=$isAccepted,rentRequestId=${rentRequestId}, requesterUserId=${userId}}")
            throw productIdUrlBodyMismatchException(
                productId,
                rentRequest.requestedProductId!!
            )
        }
        if (product.ownerOfProductUserId != userId) {
            log.error("Error trying to accept/reject rent request because of not allowed access. {error=NotAllowedAccessToResourceException, isAccepted=$isAccepted,rentRequestId=${rentRequestId}, requesterUserId=${userId}}")
            throw NotAllowedAccessToResourceException(userId)
        }
        if (isAccepted) {
            var updatedModel = addRentToProduct(product, rentRequest)
            var elasticProductModel = ObjectMapperUtils.map(updatedModel, ElasticProductModel::class.java)
            elasticProductService.save(elasticProductModel)
        }
        return rentRequestService.updateStatus(newStatus = if (isAccepted) "accepted" else "rejected", rentRequest)
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

    override fun getProductsWithRentRequestsForUser(userId: String): MutableList<UserProductsWithRentRequestsView> {
        val productsOfferedByUser = productRepository.findByOwnerOfProductUserId(userId)
        val rentRequestsForProductsOfUser = rentRequestRepository.findByRentRequestReceiverUserId(userId)

        val productsWithRentRequests: MutableList<UserProductsWithRentRequestsView> = mutableListOf()
        for (product in productsOfferedByUser) {
            val rentRequestsForProducts =
                rentRequestsForProductsOfUser.filter { it.requestedProductId == product.id.toString() }
            val currentProductRentView = ObjectMapperUtils.map(product, UserProductsWithRentRequestsView::class.java)
            currentProductRentView.rentRequests =
                ObjectMapperUtils.mapAll(rentRequestsForProducts, RentRequestView::class.java)
            productsWithRentRequests.add(currentProductRentView)
        }
        return productsWithRentRequests
    }

    override fun getRentRequestsWithProducts(userId: String): MutableList<UserSentRentRequestsWithProductsView> {
        val rentRequestsByUser = rentRequestRepository.findByRequesterUserId(userId)
        val rentRequestsIds: List<String> = rentRequestsByUser.map { it.requestedProductId!! }
        var requestedProducts = productRepository.findByIdIn(rentRequestsIds)

        val rentRequestsWithProduct: MutableList<UserSentRentRequestsWithProductsView> = mutableListOf()
        for (rentRequest in rentRequestsByUser) {
            val productOfRentRequest = requestedProducts.find { it.id == rentRequest.requestedProductId }
            rentRequestsWithProduct.add(
                UserSentRentRequestsWithProductsView(
                    ObjectMapperUtils.map(
                        rentRequest,
                        RentRequestView::class.java
                    ), ObjectMapperUtils.map(productOfRentRequest, ProductView::class.java)
                )
            )
        }
        return rentRequestsWithProduct
    }
}
