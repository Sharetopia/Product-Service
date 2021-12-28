package de.sharetopia.productservice.product.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ProductExceptionHandler {
    @ExceptionHandler
    fun handleException(exc : HttpException) : ResponseEntity<ProductErrorResponse> {
        val productErrorResponse = ProductErrorResponse(
            exc.errorCode,
            exc.message ?: "Product not found",
            System.currentTimeMillis())

        return ResponseEntity(productErrorResponse, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler
    fun handleGenericException(exc : Exception) : ResponseEntity<ProductErrorResponse> {
        val productErrorResponse = ProductErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
            exc.message ?: "Internal Server Error",
            System.currentTimeMillis())

        return ResponseEntity(productErrorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}