package de.sharetopia.productservice.product.model

import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.LocalDate

class DateRangeDuration(@Field(name = "gte", type = FieldType.Date)
                   public var fromDate: LocalDate? = null,
                        @Field(name = "lte", type = FieldType.Date)
                   public var toDate: LocalDate? = null)