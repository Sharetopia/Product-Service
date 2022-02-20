package de.sharetopia.productservice.product.service

import de.sharetopia.productservice.product.dto.ProductDTO
import de.sharetopia.productservice.product.dto.UserProductsWithRentRequestsView
import de.sharetopia.productservice.product.dto.UserSentRentRequestsWithProductsView
import de.sharetopia.productservice.product.model.ProductModel
import de.sharetopia.productservice.product.model.RentRequestModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductService {
    fun findAll(): List<ProductModel>

    fun create(product: ProductModel, userId: String): ProductModel

    fun updateOrInsert(productId: String, product: ProductModel, userId: String): ProductModel

    fun partialUpdate(
        productId: String,
        updatedFieldsProductDTO: ProductDTO,
        userId: String
    ): ProductModel

    fun findById(productId: String): ProductModel

    fun deleteById(productId: String, userId: String)

    fun findManyById(ids: List<String>, pageable: Pageable): Page<ProductModel>

    fun acceptOrRejectRentRequest(
        productId: String,
        rentRequestId: String,
        isAccepted: Boolean,
        userId: String
    ): RentRequestModel

    fun addRentToProduct(product: ProductModel, rentRequest: RentRequestModel): ProductModel

    fun getProductsWithRentRequestsForUser(userId: String): MutableList<UserProductsWithRentRequestsView>

    fun getRentRequestsWithProducts(userId: String): MutableList<UserSentRentRequestsWithProductsView>

}
