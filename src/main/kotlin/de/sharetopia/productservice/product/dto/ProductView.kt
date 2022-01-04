package de.sharetopia.productservice.product.dto

import de.sharetopia.productservice.product.model.Address
import de.sharetopia.productservice.product.model.Rent
import de.sharetopia.productservice.product.model.DateRangeDuration
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

//Server to client
class ProductView {
    lateinit var id: String
    lateinit var ownerOfProductUserId: String
    lateinit var title: String
    lateinit var description: String
    lateinit var tags: List<String>
    lateinit var address: Address
    @ArraySchema( arraySchema =  Schema(
        description = "Longitude/Latitude",
        example ="[9.430380, 48.923069]"))
    lateinit var location: List<Double>
    lateinit var rentableDateRange: DateRangeDuration
    lateinit var rents: List<Rent>
}