package fi.metatavu.lipsanen.functional.impl

import fi.metatavu.jaxrs.test.functional.builder.auth.AccessTokenProvider
import fi.metatavu.lipsanen.functional.TestBuilder
import fi.metatavu.lipsanen.functional.settings.ApiTestSettings
import fi.metatavu.lipsanen.test.client.apis.UsersApi
import fi.metatavu.lipsanen.test.client.infrastructure.ApiClient
import fi.metatavu.lipsanen.test.client.infrastructure.ClientException
import fi.metatavu.lipsanen.test.client.models.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.fail
import java.util.*

/**
 * Test builder resource for users API
 */
class UserTestBuilderResource (
    testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<User, UsersApi>(testBuilder, apiClient) {

    val user1Id = UUID.fromString("f4c1e6a1-705a-471a-825d-1982b5112ebd")

    override fun clean(p0: User?) {
        p0?.id?.let { api.deleteUser(it) }
    }

    override fun getApi(): UsersApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return UsersApi(ApiTestSettings.apiBasePath)
    }

    /**
     * Creates a new user
     *
     * @param user user
     * @return created user
     */
    fun create(user: User): User {
        return addClosable(api.createUser(user))
    }

    fun create(username: String): User {
        return addClosable(api.createUser(User(
            email = "$username@example.com",
            firstName = "username",
            lastName = "username"
        )))
    }

    /**
     * Finds a user
     *
     * @param userId user id
     * @param includeRoles
     * @return found user
     */
    fun findUser(userId: UUID): User {
        return api.findUser(userId)
    }

    /**
     * Lists users
     *
     * @param first first
     * @param max max
     * @param includeRoles
     * @return list of users
     */
    fun listUsers(
        companyId: UUID? = null,
        first: Int? = null,
        max: Int? = null
    ): Array<User> {
        return api.listUsers(companyId = companyId, first = first, max = max)
    }

    /**
     * Updates a user
     *
     * @param userId user id
     * @param user user
     * @return updated user
     */
    fun updateUser(userId: UUID, user: User): User {
        return api.updateUser(userId, user)
    }

    /**
     * Deletes a user
     *
     * @param userId user id
     */
    fun deleteUser(userId: UUID) {
        api.deleteUser(userId)
        removeCloseable { closable: Any ->
            if (closable !is User) {
                return@removeCloseable false
            }
            closable.id == userId
        }
    }

    /**
     * Asserts that create fails with expected status
     *
     * @param expectedStatus expected status
     * @param user user
     */
    fun assertCreateFailStatus(
        expectedStatus: Int,
        user: User
    ) {
        try {
            api.createUser(user)
            fail(String.format("Expected create to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    /**
     * Asserts that find fails with expected status
     *
     * @param expectedStatus expected status
     * @param userId user id
     */
    fun assertFindFailStatus(
        expectedStatus: Int,
        userId: UUID
    ) {
        try {
            api.findUser(userId)
            fail(String.format("Expected find to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    /**
     * Asserts that update fails with expected status
     *
     * @param expectedStatus expected status
     * @param userId user id
     * @param user user
     */
    fun assertUpdateFailStatus(
        expectedStatus: Int,
        userId: UUID,
        user: User
    ) {
        try {
            api.updateUser(userId, user)
            fail(String.format("Expected update to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    /**
     * Asserts that delete fails with expected status
     *
     * @param expectedStatus expected status
     * @param userId user id
     */
    fun assertDeleteFailStatus(
        expectedStatus: Int,
        userId: UUID
    ) {
        try {
            api.deleteUser(userId)
            fail(String.format("Expected delete to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }

    /**
     * Asserts that list fails with expected status
     *
     * @param expectedStatus expected status
     */
    fun assertListFailStatus(
        expectedStatus: Int
    ) {
        try {
            api.listUsers()
            fail(String.format("Expected list to fail with status %d", expectedStatus))
        } catch (e: ClientException) {
            Assertions.assertEquals(expectedStatus.toLong(), e.statusCode.toLong())
        }
    }
}