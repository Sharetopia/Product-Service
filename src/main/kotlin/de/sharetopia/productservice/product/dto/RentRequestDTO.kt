package de.sharetopia.productservice.product.dto

import java.time.LocalDate

data class RentRequestDTO(
    var fromDate: LocalDate? = null,
    var toDate: LocalDate? = null,
    var requestedProductId: String? = null,
){
}