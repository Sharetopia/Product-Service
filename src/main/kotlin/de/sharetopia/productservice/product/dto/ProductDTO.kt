package de.sharetopia.productservice.product.dto

import de.sharetopia.productservice.product.model.Address
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

//Client to server
data class ProductDTO(var title: String? = null,
                      var description: String? = null,
                      var tags: List<String>? = null,
                      var address: Address? = null,
                      @ArraySchema( arraySchema =  Schema(
                          description = "Longitude/Latitude",
                          example ="[9.430380, 48.923069]"))
                      var location: List<Double>? = null){
}