package de.sharetopia.productservice.product.dto

import org.springframework.data.annotation.Id

//von client an server => andersrum ist view
data class ProductDTO(var title: String? = null, var description: String? = null, var tags: List<String>? = null){
}