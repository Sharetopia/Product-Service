package de.sharetopia.productservice.product.exception

import org.springframework.http.HttpStatus

class ProductNotFoundException(id: String): HttpException("Product with id $id was not found"){
    override val errorCode: HttpStatus = HttpStatus.NOT_FOUND
}