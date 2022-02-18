package de.sharetopia.productservice.product.model

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.*
import java.math.BigDecimal


@Document(indexName = "product")
@Setting(settingPath = "settings/settings.json")
class ElasticProductModel {

    @Id
    var id: String? = null

    @Field(type = FieldType.Text, name = "ownerOfProductUserId")
    var ownerOfProductUserId: String = ""

    @Field(type = FieldType.Search_As_You_Type, name = "title", analyzer = "rebuilt_german")
    var title: String = ""

    @Field(type = FieldType.Text, name = "description", analyzer = "rebuilt_german")
    var description: String = ""

    @Field(type = FieldType.Search_As_You_Type, name = "tags", analyzer = "rebuilt_german")
    var tags: List<String> = listOf()

    @Field(type = FieldType.Float, name = "price")
    var price: BigDecimal = BigDecimal.ZERO

    @Field(type = FieldType.Object, name = "address")
    var address: Address = Address("", "", "")

    @GeoPointField
    var location: DoubleArray? = null

    @Field(type = FieldType.Date_Range, name = "rentableDateRange")
    var rentableDateRange: DateRangeDuration? = null

    @Field(type = FieldType.Nested, name = "rents")
    var rents: List<Rent>? = null

}