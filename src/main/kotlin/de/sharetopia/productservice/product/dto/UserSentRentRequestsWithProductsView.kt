package de.sharetopia.productservice.product.dto

import de.sharetopia.productservice.product.model.ProductModel
import de.sharetopia.productservice.product.model.RentRequestModel

data class UserSentRentRequestsWithProductsView(var rentRequest: RentRequestModel,
                                                var product: ProductModel
                                                ) {
}