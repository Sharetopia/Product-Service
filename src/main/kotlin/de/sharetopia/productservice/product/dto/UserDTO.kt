package de.sharetopia.productservice.product.dto

data class UserDTO(
    var profilePictureURL: String? = null,
    var forename: String? = null,
    var surname: String? = null,
    var address: String? = null,
    var city: String? = null,
    var rating: String? = null,
    var postalCode: String? = null
)