package de.sharetopia.productservice.product.dto

import de.sharetopia.productservice.product.model.Address

//Client to server
data class ProductDTO(var title: String? = null,
                      var description: String? = null,
                      var tags: List<String>? = null,
                      var address: Address? = null,
                      var location: List<Double>? = null){
}