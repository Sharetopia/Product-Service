package de.sharetopia.productservice.product.dto

data class UserView(
    var id: String = "",
    var profilePictureURL: String = "",
    var forename: String = "",
    var surname: String = "",
    var address: String = "",
    var city: String = "",
    var rating: String = "",
    var postalCode: String = "",
)