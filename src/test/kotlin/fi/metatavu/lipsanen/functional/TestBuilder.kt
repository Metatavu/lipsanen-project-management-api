package fi.metatavu.lipsanen.functional

import fi.metatavu.jaxrs.test.functional.builder.AbstractAccessTokenTestBuilder
import fi.metatavu.jaxrs.test.functional.builder.AbstractTestBuilder
import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.jaxrs.test.functional.builder.auth.AuthorizedTestBuilderAuthentication
import fi.metatavu.jaxrs.test.functional.builder.auth.KeycloakAccessTokenProvider
import fi.metatavu.lipsanen.functional.auth.TestBuilderAuthentication
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import org.eclipse.microprofile.config.ConfigProvider

/**
 * Abstract test builder class
 *
 * @author Jari Nykänen
 * @author Antti Leppä
 */
class TestBuilder(private val config: Map<String, String>): AbstractAccessTokenTestBuilder<ApiClient>() {

    val admin = createTestBuilderAuthentication(username = "admin", password = "test")
    val user = createTestBuilderAuthentication(username = "user", password = "test")
    val user1 = createTestBuilderAuthentication(username = "user1", password = "test")
    val user2 = createTestBuilderAuthentication(username = "user2", password = "test")

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
        val serverUrl = ConfigProvider.getConfig().getValue("quarkus.oidc.auth-server-url", String::class.java).substringBefore("/realms")
        val realm: String = ConfigProvider.getConfig().getValue("lipsanen.keycloak.admin.realm", String::class.java)
        val clientId = "api"
        val secret: String = ConfigProvider.getConfig().getValue("lipsanen.keycloak.admin.secret", String::class.java)
        return TestBuilderAuthentication(this, KeycloakAccessTokenProvider(serverUrl, realm, clientId, username, password, secret))
    }

}