package fi.metatavu.lipsanen.keycloak
import fi.metatavu.keycloak.adminclient.apis.RealmAdminApi
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*

import fi.metatavu.keycloak.adminclient.apis.UserApi
import fi.metatavu.keycloak.adminclient.apis.UsersApi
import fi.metatavu.keycloak.adminclient.models.UserRepresentation

/**
 * Controller for accessing Keycloak as admin user
 */
@ApplicationScoped
class KeycloakAdminClient : KeycloakClient() {
    override var clientType = KeycloakClientType.ADMIN

    @ConfigProperty(name = "lipsanen.keycloak.admin.secret")
    lateinit var keycloakAdminClientSecret: String

    @ConfigProperty(name = "lipsanen.keycloak.admin.client")
    lateinit var keycloakAdminClientId: String

    @ConfigProperty(name = "lipsanen.keycloak.admin.password")
    lateinit var keycloakAdminPassword: String

    @ConfigProperty(name = "lipsanen.keycloak.admin.user")
    lateinit var keycloakAdminUser: String

    @Inject
    lateinit var vertxCore: io.vertx.core.Vertx

    /**
     * Finds a user by id
     *
     * @param userId user id
     * @return found user or null if not found
     */
    suspend fun findUserById(userId: UUID): UserRepresentation? {
        return getUserApi().realmUsersIdGet(realm = getRealm(), id = userId.toString())
    }

    /**
     * Requests a new access token
     *
     * @return new access token
     */
    override fun requestNewToken(): Uni<KeycloakAccessToken> {
        return sendTokenRequest(
            keycloakAdminClientId,
            keycloakAdminClientSecret,
            keycloakAdminUser,
            keycloakAdminPassword
        )
    }

    /**
     * Gets user api with valid access token
     *
     * @return Api with valid access token
     */
    suspend fun getUserApi(): UserApi {
        val baseUrl = getBaseUrl()
        return UserApi(
            basePath = "${baseUrl}/admin/realms",
            accessToken = getAccessToken(),
            vertx = vertxCore
        )
    }

    /**
     * Gets realm admin api
     *
     * @return Api with valid access token
     */
    suspend fun getRealmAdminApi(): RealmAdminApi {
        val baseUrl = getBaseUrl()
        return RealmAdminApi(
            basePath = "${baseUrl}/admin/realms",
            accessToken = getAccessToken(),
            vertx = vertxCore
        )
    }

    /**
     * Gets users api with valid access token
     *
     * @return Api with valid access token
     */
    suspend fun getUsersApi(): UsersApi {
        val baseUrl = getBaseUrl()
        return UsersApi(
            basePath = "${baseUrl}/admin/realms",
            accessToken = getAccessToken(),
            vertx = vertxCore
        )
    }

    /**
     * Gets base url
     *
     * @return base url
     */
    private fun getBaseUrl(): String {
        return keycloakUrl.substringBefore("/realms")
    }

    /**
     * Gets realm name
     *
     * @return realm name
     */
    fun getRealm(): String {
        return keycloakUrl.substringAfterLast("realms/").substringBefore("/")
    }

}