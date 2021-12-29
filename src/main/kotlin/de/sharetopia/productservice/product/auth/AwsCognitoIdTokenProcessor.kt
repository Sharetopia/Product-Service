package de.sharetopia.productservice.product.auth

import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import java.util.List
import javax.servlet.http.HttpServletRequest

@Component
class AwsCognitoIdTokenProcessor {
    @Autowired
    private lateinit var jwtConfiguration: JwtConfiguration

    @Autowired
    private lateinit var configurableJWTProcessor: ConfigurableJWTProcessor<SecurityContext>

    @Throws(Exception::class)
    fun authenticate(request: HttpServletRequest): Authentication? {
        val idToken = request.getHeader(jwtConfiguration.httpHeader)
        if (idToken != null) {
            val claims = configurableJWTProcessor.process(getBearerToken(idToken), null)
            validateIssuer(claims)
            verifyIfIdToken(claims)
            val username = getUserNameFrom(claims)
            if (username != null) {
                val grantedAuthorities = List.of<GrantedAuthority>(SimpleGrantedAuthority("ROLE_USER"))
                val user = User(username, "", List.of())
                return JwtAuthentication(user, claims, grantedAuthorities)
            }
        }
        return null
    }

    private fun getUserNameFrom(claims: JWTClaimsSet): String {
        return claims.claims[jwtConfiguration.userNameField].toString()
    }

    @Throws(Exception::class)
    private fun verifyIfIdToken(claims: JWTClaimsSet) {
        if (claims.issuer != jwtConfiguration.cognitoIdentityPoolUrl) {
            throw Exception("JWT Token is not an ID Token")
        }
    }

    @Throws(Exception::class)
    private fun validateIssuer(claims: JWTClaimsSet) {
        if (claims.issuer != jwtConfiguration.cognitoIdentityPoolUrl) {
            throw Exception(
                java.lang.String.format(
                    "Issuer %s does not match cognito idp %s",
                    claims.issuer,
                    jwtConfiguration.cognitoIdentityPoolUrl
                )
            )
        }
    }

    private fun getBearerToken(token: String): String {
        return if (token.startsWith("Bearer ")) token.substring("Bearer ".length) else token
    }
}