package de.sharetopia.productservice.product.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ProductExceptionHandler {
    @ExceptionHandler
    fun handleException(exc : ProductNotFoundException) : ResponseEntity<ProductErrorResponse> {
        val productErrorResponse = ProductErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            exc.message,
            System.currentTimeMillis())

        return ResponseEntity(productErrorResponse, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler
    fun handleGenericException(exc : Exception) : ResponseEntity<ProductErrorResponse> {
        val productErrorResponse = ProductErrorResponse(HttpStatus.BAD_REQUEST.value(),
            exc.message,
            System.currentTimeMillis())

        return ResponseEntity(productErrorResponse, HttpStatus.BAD_REQUEST)
    }
}