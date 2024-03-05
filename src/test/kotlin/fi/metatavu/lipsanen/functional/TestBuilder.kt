package fi.metatavu.lipsanen.functional

import fi.metatavu.jaxrs.test.functional.builder.AbstractAccessTokenTestBuilder
import fi.metatavu.jaxrs.test.functional.builder.AbstractTestBuilder
import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.jaxrs.test.functional.builder.auth.AuthorizedTestBuilderAuthentication
import fi.metatavu.jaxrs.test.functional.builder.auth.KeycloakAccessTokenProvider
import fi.metatavu.lipsanen.functional.auth.TestBuilderAuthentication
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient

/**
 * Abstract test builder class
 *
 * @author Jari Nykänen
 * @author Antti Leppä
 */
class TestBuilder(private val config: Map<String, String>): AbstractAccessTokenTestBuilder<ApiClient>() {

    var user = createTestBuilderAuthentication(username = "user", password = "userPassword")

    override fun createTestBuilderAuthentication(
        abstractTestBuilder: AbstractTestBuilder<ApiClient, AccessTokenProvider>,
        authProvider: AccessTokenProvider
    ): AuthorizedTestBuilderAuthentication<ApiClient, AccessTokenProvider> {
        return TestBuilderAuthentication(this, authProvider)
    }

    /**
     * Creates test builder authenticatior for given user
     *
     * @param username username
     * @param password password
     * @return test builder authenticatior for given user
     */
    private fun createTestBuilderAuthentication(username: String, password: String): TestBuilderAuthentication {
        val serverUrl = config["quarkus.oidc.auth-server-url"]!!.substringBeforeLast("/").substringBeforeLast("/")
        val realm: String = config["quarkus.keycloak.devservices.realm-name"].toString()
        val clientId = "test"
        val clientSecret = "secret"
        return TestBuilderAuthentication(this, KeycloakAccessTokenProvider(serverUrl, realm, clientId, username, password, clientSecret))
    }

}