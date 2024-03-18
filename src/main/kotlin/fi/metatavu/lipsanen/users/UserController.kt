package fi.metatavu.lipsanen.users

import fi.metatavu.keycloak.adminclient.models.EventRepresentation
import fi.metatavu.keycloak.adminclient.models.UserRepresentation
import fi.metatavu.lipsanen.api.model.User
import fi.metatavu.lipsanen.keycloak.KeycloakAdminClient
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.util.*

/**
 * Controller for managing users
 */
@ApplicationScoped
class UserController {

    @Inject
    lateinit var keycloakAdminClient: KeycloakAdminClient

    @Inject
    lateinit var logger: Logger

    /**
     * Lists users
     *
     * @param first first result
     * @param max max results
     * @return users
     */
    suspend fun listUsers(first: Int?, max: Int?): Pair<Array<UserRepresentation>, Int> {
        val users = keycloakAdminClient.getUsersApi().realmUsersGet(
            realm = keycloakAdminClient.getRealm(),
            first = first,
            max = max
        )
        val usersCount = keycloakAdminClient.getUsersApi().realmUsersCountGet(
            realm = keycloakAdminClient.getRealm()
        )
        return users to usersCount
    }

    /**
     * Creates a new user with unconfirmed password and email and sends the
     * email with reset password action to the user
     *
     * @param user user
     * @return created user
     */
    suspend fun createUser(user: User): UserRepresentation? {
        return try {
            keycloakAdminClient.getUsersApi().realmUsersPost(
                realm = keycloakAdminClient.getRealm(),
                userRepresentation = UserRepresentation(
                    firstName = user.firstName,
                    lastName = user.lastName,
                    email = user.email,
                    username = user.email,
                    enabled = true
                )
            )
            val createdUser = keycloakAdminClient.getUsersApi().realmUsersGet(
                realm = keycloakAdminClient.getRealm(),
                email = user.email
            ).firstOrNull() ?: return null

            keycloakAdminClient.getUserApi().realmUsersIdExecuteActionsEmailPut(
                realm = keycloakAdminClient.getRealm(),
                id = createdUser.id!!,
                requestBody = arrayOf("UPDATE_PASSWORD"),
                lifespan = null
            )

            createdUser
        } catch (e: Exception) {
            logger.error("Failed to create user:", e)
            null
        }
    }


    /**
     * Finds a user by id
     *
     * @param userId user id
     * @return found user or null if not found
     */
    suspend fun findUser(userId: UUID): UserRepresentation? {
        return try {
            keycloakAdminClient.getUserApi().realmUsersIdGet(
                realm = keycloakAdminClient.getRealm(),
                id = userId.toString()
            )
        } catch (e: Exception) {
            logger.error("Failed to find user", e)
            null
        }
    }

    /**
     * Updates a user (but not its email)
     *
     * @param userId user id
     * @param existingUser existing user
     * @param updateData user
     * @return updated user
     */
    suspend fun updateUser(userId: UUID, existingUser: UserRepresentation, updateData: User): UserRepresentation? {
        val updatedRepresentation = existingUser.copy(
            firstName = updateData.firstName,
            lastName = updateData.lastName,
        )
        return try {
            keycloakAdminClient.getUserApi().realmUsersIdPut(
                realm = keycloakAdminClient.getRealm(),
                id = userId.toString(),
                userRepresentation = updatedRepresentation
            )
            keycloakAdminClient.getUserApi().realmUsersIdGet(
                realm = keycloakAdminClient.getRealm(),
                id = userId.toString()
            )
        } catch (e: Exception) {
            logger.error("Failed to update user", e)
            null
        }
    }

    /**
     * Deletes a user
     *
     * @param userId user id
     */
    suspend fun deleteUser(userId: UUID) {
        try {
            keycloakAdminClient.getUserApi().realmUsersIdDelete(
                realm = keycloakAdminClient.getRealm(),
                id = userId.toString()
            )
        } catch (e: Exception) {
            logger.error("Failed to delete user", e)
        }
    }

    /**
     * Counts users by email
     *
     * @param email email
     * @return count
     */
    suspend fun countUserByEmail(email: String): Int {
        return try {
            keycloakAdminClient.getUsersApi().realmUsersCountGet(
                realm = keycloakAdminClient.getRealm(),
                email = email
            )
        } catch (e: Exception) {
            logger.error("Failed to find users", e)
            0
        }
    }

    /**
     * Gets user's last login event
     *
     * @param userId user id
     * @return last login event or null if not found
     */
    suspend fun getLastLogin(userId: UUID): EventRepresentation? {
        return try {
            keycloakAdminClient.getRealmAdminApi().realmEventsGet(
                realm = keycloakAdminClient.getRealm(),
                type = arrayOf("LOGIN"),
                user = userId.toString(),
            ).sortedByDescending { it.time }.firstOrNull()
        } catch (e: Exception) {
            logger.error("Failed to get user's last login", e)
            null
        }
    }
}