package de.sharetopia.productservice.product.auth

import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.io.IOException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest

@Component
class AwsCognitoJwtAuthFilter : GenericFilter() {
    @Autowired
    private lateinit var cognitoIdTokenProcessor: AwsCognitoIdTokenProcessor

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, filterChain: FilterChain) {
        val authentication: Authentication?
        try {
            authentication = cognitoIdTokenProcessor.authenticate(request as HttpServletRequest)
            if (authentication != null) {
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (e: Exception) {
            logger.error("Cognito ID Token processing error", e)
            SecurityContextHolder.clearContext()
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        private val logger = LogFactory.getLog(
            AwsCognitoJwtAuthFilter::class.java
        )
    }
}