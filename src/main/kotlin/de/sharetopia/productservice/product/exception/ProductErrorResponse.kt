package de.sharetopia.productservice.product.exception

import org.springframework.http.HttpStatus

class ProductErrorResponse(val status : HttpStatus,
                           val message : String,
                           val timeStamp : Long) {
}