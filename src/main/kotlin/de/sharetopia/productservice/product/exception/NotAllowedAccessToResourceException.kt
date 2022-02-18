package de.sharetopia.productservice.product.exception

import org.springframework.http.HttpStatus

class NotAllowedAccessToResourceException(userId: String) :
    HttpException("Currently authorized user with id $userId is not allowed to access this resource") {
    override val errorCode: HttpStatus = HttpStatus.FORBIDDEN
}