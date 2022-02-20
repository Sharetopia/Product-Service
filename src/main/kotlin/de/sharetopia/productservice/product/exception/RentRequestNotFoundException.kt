package de.sharetopia.productservice.product.exception

import org.springframework.http.HttpStatus

class RentRequestNotFoundException(id: String) : HttpException("Rent request with id $id was not found") {
    override val errorCode: HttpStatus = HttpStatus.NOT_FOUND
}