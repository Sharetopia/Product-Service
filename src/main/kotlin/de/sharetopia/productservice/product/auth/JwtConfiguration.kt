package de.sharetopia.productservice.product.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "com.sharetopia.aws")
class JwtConfiguration {
    var userPoolId: String? = null
    var identityPoolId: String? = null
    var jwkUrl: String? = null
        get() = if (field != null && !field!!.isEmpty()) field else String.format(
            "https://cognito-idp.%s.amazonaws.com/%s/.well-known/jwks.json",
            region, userPoolId
        )
    var region = "eu-central-1"
    var userNameField = "username"
    var connectionTimeout = 2000
    var readTimeout = 2000
    var httpHeader = "Authorization"
    val cognitoIdentityPoolUrl: String
        get() = String.format("https://cognito-idp.%s.amazonaws.com/%s", region, userPoolId)
}