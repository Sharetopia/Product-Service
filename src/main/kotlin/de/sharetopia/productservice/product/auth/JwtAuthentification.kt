package de.sharetopia.productservice.product.auth

import com.nimbusds.jwt.JWTClaimsSet
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class JwtAuthentication(
    private val principal: Any,
    val jwtClaimsSet: JWTClaimsSet,
    authorities: Collection<GrantedAuthority?>?
) :
    AbstractAuthenticationToken(authorities) {

    override fun getCredentials(): Any? {
        return null
    }

    override fun getPrincipal(): Any {
        return principal
    }

    init {
        super.setAuthenticated(true)
    }
}