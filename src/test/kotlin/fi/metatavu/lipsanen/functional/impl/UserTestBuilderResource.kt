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
 * Test builder resource for users
 */
class UserTestBuilderResource (
    testBuilder: TestBuilder,
    private val accessTokenProvider: AccessTokenProvider?,
    apiClient: ApiClient
) : ApiTestBuilderResource<User, UsersApi>(testBuilder, apiClient) {
    override fun clean(p0: User?) {
        p0?.id?.let { api.deleteUser(it) }
    }

    override fun getApi(): UsersApi {
        ApiClient.accessToken = accessTokenProvider?.accessToken
        return UsersApi(ApiTestSettings.apiBasePath)
    }

    fun create(user: User): User {
        return addClosable(api.createUser(user))
    }

    fun findUser(userId: UUID): User {
        return api.findUser(userId)
    }

    fun listUsers(
        first: Int? = null,
        max: Int? = null
    ): Array<User> {
        return api.listUsers(first = first, max = max)
    }

    fun updateUser(userId: UUID, user: User): User {
        return api.updateUser(userId, user)
    }

    fun deleteUser(userId: UUID) {
        api.deleteUser(userId)
        removeCloseable { closable: Any ->
            if (closable !is User) {
                return@removeCloseable false
            }
            closable.id == userId
        }
    }

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