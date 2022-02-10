package de.sharetopia.productservice.product.exception

import org.springframework.http.HttpStatus

class productIdUrlBodyMismatchException(productIdInUrl: String, productIdInBody: String): HttpException("In url provided product id $productIdInUrl mismatches product id $productIdInBody contained in body") {
    override val errorCode: HttpStatus = HttpStatus.BAD_REQUEST
}