package de.sharetopia.productservice.product.model

import org.elasticsearch.common.geo.GeoJson
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.*
import org.springframework.data.elasticsearch.core.geo.GeoJsonPoint
import org.springframework.data.elasticsearch.core.geo.GeoPoint



@Document(indexName = "product")
@Setting(settingPath = "settings/settings.json")
public class ElasticProductModel{

    @Id var id: String? = null

    @Field(type = FieldType.Search_As_You_Type, name = "title", analyzer = "rebuilt_german")
    var title: String=""

    @Field(type = FieldType.Text, name = "description", analyzer = "rebuilt_german")
    var description: String=""

    @Field(type = FieldType.Text, name = "tags")
    var tags: List<String> = listOf()

    @Field(type = FieldType.Object, name = "address")
    var address: Address = Address("","","")

    @GeoPointField
    var location: DoubleArray? = null

}