package de.sharetopia.productservice.testUtil

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder
import com.amazonaws.services.cognitoidp.model.AuthFlowType
import com.amazonaws.services.cognitoidp.model.AuthenticationResultType
import com.amazonaws.services.cognitoidp.model.InitiateAuthRequest

class AuthorizationUtils {

    val testRegion = "eu-central-1"
    val testUserPoolId = "eu-central-1_sxdpyi2QD"
    val testClientId = "3vgfb9n80chgbah3lvqiq7l9f0"

    fun getAmazonCognitoIdentityClient(): AWSCognitoIdentityProvider? {
        val awsCreds = AnonymousAWSCredentials()
        val provider = AWSCognitoIdentityProviderClientBuilder
            .standard()
            .withCredentials(AWSStaticCredentialsProvider(awsCreds))
            .withRegion(testRegion)
            .build()
        return provider
    }

    fun login(username: String, password: String): String {
        var authenticationResult: AuthenticationResultType?
        val cognitoClient = getAmazonCognitoIdentityClient()

        val authParams: MutableMap<String, String> = HashMap()
        authParams["USERNAME"] = username
        authParams["PASSWORD"] = password

        val authRequest = InitiateAuthRequest()
        authRequest.withAuthFlow(AuthFlowType.USER_PASSWORD_AUTH)
            .withClientId(testClientId)
            .withAuthParameters(authParams)

        val result = cognitoClient!!.initiateAuth(authRequest)


        authenticationResult = result.authenticationResult
        cognitoClient.shutdown()
        return authenticationResult.accessToken
    }
}