package de.sharetopia.productservice.product.exception

import org.springframework.http.HttpStatus

abstract class HttpException(message: String): RuntimeException(message) {
    abstract val errorCode: HttpStatus
}