package de.sharetopia.productservice.product.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ProductExceptionHandler {
    @ExceptionHandler(
        LocationNotFoundException::class,
        ProductNotFoundException::class,
        RentRequestNotFoundException::class,
        UserNotFoundException::class
    )
    fun handleNotFoundException(exc : HttpException) : ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            exc.errorCode,
            exc.message ?: "Resource not found",
            System.currentTimeMillis())

        return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(
        productIdUrlBodyMismatchException::class,
        InvalidDateRangeSearchException::class,
    )
    fun handleBadRequestException(exc : HttpException) : ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            exc.errorCode,
            exc.message ?: "Invalid request",
            System.currentTimeMillis())

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(
        NotAllowedAccessToResourceException::class
    )
    fun handleNotAllowedAccessRequestException(exc : HttpException) : ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            exc.errorCode,
            exc.message ?: "Not allowed to access this resource",
            System.currentTimeMillis())

        return ResponseEntity(errorResponse, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler
    fun handleGenericException(exc : Exception) : ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
            exc.message ?: "Internal Server Error",
            System.currentTimeMillis())

        return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}