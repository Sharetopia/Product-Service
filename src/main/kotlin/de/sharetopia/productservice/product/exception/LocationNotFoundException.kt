package de.sharetopia.productservice.product.exception

import org.springframework.http.HttpStatus

class LocationNotFoundException(locationIdentifier: String): HttpException("Could not find coordinates for provided location $locationIdentifier") {
    override val errorCode: HttpStatus = HttpStatus.NOT_FOUND
}