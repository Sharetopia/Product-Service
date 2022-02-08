package de.sharetopia.productservice.product.dto

data class UserSentRentRequestsWithProductsView(var rentRequest: RentRequestView,
                                                var product: ProductView
                                                ) {
}