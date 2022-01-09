package de.sharetopia.productservice.product.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "user")
data class UserModel(@Id var id: String = "",
                     var profilePictureURL: String = "",
                     var name: String = "",
                     var postalCode: String = ""
                     )