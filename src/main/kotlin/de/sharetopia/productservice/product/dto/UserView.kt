package de.sharetopia.productservice.product.dto

data class UserView(var id: String="",
                   var profilePictureURL: String = "",
                   var name: String = "",
                   var postalCode: String = ""
)