package de.sharetopia.productservice.product.exception

import org.springframework.http.HttpStatus

class InvalidDateRangeSearchException : HttpException("Cannot execute search for only one provided date") {
    override val errorCode: HttpStatus = HttpStatus.BAD_REQUEST
}