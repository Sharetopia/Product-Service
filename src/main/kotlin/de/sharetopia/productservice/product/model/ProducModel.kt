package de.sharetopia.productservice.product.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal


@Document(collection = "products")
data class ProductModel(
    @Id var id: String = ObjectId.get().toString(),
    var title: String = "",
    var ownerOfProductUserId: String = "",
    var description: String = "",
    var tags: List<String> = listOf(),
    var price: BigDecimal = BigDecimal.ZERO,
    var address: Address = Address("", "", ""),
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    var location: List<Double>? = null,
    var rentableDateRange: DateRangeDuration? = null,
    var rents: MutableList<Rent>? = null
)
