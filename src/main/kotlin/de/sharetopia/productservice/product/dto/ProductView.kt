package de.sharetopia.productservice.product.dto

import de.sharetopia.productservice.product.model.Address
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.geo.GeoJsonPoint

class ProductView {
    lateinit var id: String
    lateinit var title: String
    lateinit var description: String
    lateinit var tags: List<String>
    lateinit var address: Address
    lateinit var location: List<Double>
}