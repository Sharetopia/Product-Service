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

/*data class ProductView(var id: String? = null, var title: String? = null,
                      var description: String? = null,
                      var tags: List<String>? = null,
                      var address: Address? = null,
                      var location: List<Double>? = null){
}*/