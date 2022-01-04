package de.sharetopia.productservice.product.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate

@Document(collection = "rentRequest")
public data class RentRequestModel(@Id var id: ObjectId = ObjectId.get(),
                                   var fromDate: LocalDate? = null,
                                   var toDate: LocalDate? = null,
                                   var requesterUserId: String? = null,
                                   var rentRequestReceiverUserId: String? = null,
                                   var requestedProductId: String? = null,
                                   var status: String = "open"
)