package de.sharetopia.productservice.product.model

import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

class Rent(
    @Field(type = FieldType.Text, name = "renterUserId")
           var renterUserId: String? = "",
    @Field(type = FieldType.Date_Range,name = "rentDuration")
           var rentDuration: DateRangeDuration? = null,
    @Field(type = FieldType.Text,name = "rentId")
           var rentId: String? = null,
           )

