package de.sharetopia.productservice.product.dto

import org.springframework.data.annotation.Id

data class UserDTO(var profilePictureURL: String = "",
                   var name: String = "",
                   var postalCode: String = ""
)