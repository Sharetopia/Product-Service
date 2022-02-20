package de.sharetopia.productservice.product.dto

import de.sharetopia.productservice.product.model.Address
import de.sharetopia.productservice.product.model.DateRangeDuration
import de.sharetopia.productservice.product.model.Rent
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

//Client to server
data class ProductDTO(
    var title: String? = null,
    var description: String? = null,
    var tags: List<String>? = null,
    var price: BigDecimal = BigDecimal.ZERO,
    var address: Address? = null,
    @ArraySchema(
        arraySchema = Schema(
            description = "Longitude/Latitude",
            example = "[9.430380, 48.923069]"
        )
    )
    var location: List<Double>? = null,
    var rentableDateRange: DateRangeDuration? = null,
    var rents: List<Rent>? = null
)