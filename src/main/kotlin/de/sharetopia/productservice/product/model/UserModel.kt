package de.sharetopia.productservice.product.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "user")
data class UserModel(@Id var id: String = "",
                    var profilePictureURL: String = "",
                    var forename: String = "",
                    var surname: String = "",
                    var address: String = "",
                    var city: String = "",
                    var rating: String = "",
                    var postalCode: String = "",
                    )