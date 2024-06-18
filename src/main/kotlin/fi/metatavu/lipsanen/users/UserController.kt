package fi.metatavu.lipsanen.users

import fi.metatavu.keycloak.adminclient.models.EventRepresentation
import fi.metatavu.keycloak.adminclient.models.GroupRepresentation
import fi.metatavu.keycloak.adminclient.models.RoleRepresentation
import fi.metatavu.keycloak.adminclient.models.UserRepresentation
import fi.metatavu.lipsanen.api.model.User
import fi.metatavu.lipsanen.keycloak.KeycloakAdminClient
import fi.metatavu.lipsanen.rest.UserRole
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.util.*

/**
 * Controller for managing users
 *
 * User's assignment logic:
 * ROLES defined the roles of the user within the project (currently ADMIN, USER)
 * GROUPS define the groups the user belongs to (projects)
 * COMPANY_ID attribute defines the company the user belongs to,
 * ROLES+GROUPS are used for access rights checks.
 */
@ApplicationScoped
class UserController {

    @ConfigProperty(name = "environment")
    lateinit var environment: Optional<String>

    @ConfigProperty(name = "lipsanen.keycloak.admin.user")
    lateinit var keycloakAdminUser: String

    @Inject
    lateinit var keycloakAdminClient: KeycloakAdminClient

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var logger: Logger

    /**
     * Lists user roles
     *
     * @param userId user id
     * @return user roles
     */
    suspend fun getUserRealmRoles(userId: UUID): Array<RoleRepresentation> {
        return keycloakAdminClient.getRoleMapperApi().realmUsersIdRoleMappingsGet(
            id = userId.toString(),
            realm = keycloakAdminClient.getRealm(),
        ).realmMappings ?: emptyArray()
    }

    /**
     * Lists user groups
     *
     * @param keycloakUserId user id
     * @return user groups
     */
    suspend fun listUserGroups(keycloakUserId: UUID): Array<GroupRepresentation> {
        return try {
            keycloakAdminClient.getUserApi().realmUsersIdGroupsGet(
                realm = keycloakAdminClient.getRealm(),
                id = keycloakUserId.toString(),
                briefRepresentation = true
            )
        } catch (e: Exception) {
            logger.error("Failed to list user groups", e)
            emptyArray()
        }
    }

    /**
     * Lists users (except for admin user) from keycloak and local db
     * If the test mode then users are created to the local db if they are not found
     *
     * @param first first result
     * @param max max results
     * @return users
     */
    suspend fun listUsers(companyId: UUID?, first: Int?, max: Int?): Pair<Array<UserFullRepresentation>, Int> {
        var users = keycloakAdminClient.getUsersApi().realmUsersGet(
            realm = keycloakAdminClient.getRealm(),
            first = first ?: 0,
            max = max?.plus(1)  ?: 10,  // +1 to account for possible admin user that has to be removed
            q = companyId?.let { "$COMPANY_PROP_NAME:$it" }
        )
        //todo company id to the user entity

        var usersCount = if(companyId == null ) {
            keycloakAdminClient.getUsersApi().realmUsersCountGet(
                realm = keycloakAdminClient.getRealm()
            )
        } else {
            keycloakAdminClient.getUsersApi().realmUsersGet(
                realm = keycloakAdminClient.getRealm(),
                q = "$COMPANY_PROP_NAME:$companyId"
            ).size
        }

        // Removing admin user from the list
        if (users.find { it.username == keycloakAdminUser } != null) {
            users = users.filter { it.username != keycloakAdminUser }.toTypedArray()
            usersCount -= 1
        } else {
            if (max != null && users.size > max) {
                users = users.slice(0 until if (max >= users.size) users.size-1 else max).toTypedArray()
            }
        }

        val userFullRepresentations = users.map {
            var userEntity = userRepository.findByKeycloakId(UUID.fromString(it.id))
            if (userEntity == null && environment.isPresent && environment.get() == "test"){
                userEntity = userRepository.create(
                    id = UUID.randomUUID(),
                    keycloakId = UUID.fromString(it.id)
                )
            }

            if (userEntity == null) {
                return@map null
            }
            UserFullRepresentation(
                userEntity = userEntity,
                userRepresentation = it
            )
        }.filterNotNull().toTypedArray()
        return userFullRepresentations to usersCount
    }

    /**
     * Lists users
     *
     * @param companyId company id
     * @return array of users
     */
    suspend fun listUsers(companyId: UUID?): Array<UserRepresentation> {
        val users = keycloakAdminClient.getUsersApi().realmUsersGet(
            realm = keycloakAdminClient.getRealm(),
            q = companyId?.let { "$COMPANY_PROP_NAME:$it" }
        )

        return users
    }

    /**
     * Finds the admins
     */
    suspend fun getAdmins(): Array<UserRepresentation> {
        return keycloakAdminClient.getRoleContainerApi().realmRolesRoleNameUsersGet(
            realm = keycloakAdminClient.getRealm(),
            roleName = "admin"
        )
    }

    /**
     * Creates a new user with unconfirmed password and email and sends the
     * email with reset password action to the user
     *
     * @param user user
     * @param groupIds user group ids to assign to
     * @return created user
     */
    suspend fun createUser(user: User, groupIds: List<UUID>?): UserFullRepresentation? {
        val keycloakUser: UserRepresentation?
        var userEntity: UserEntity? = null
        try {
            keycloakAdminClient.getUsersApi().realmUsersPost(
                realm = keycloakAdminClient.getRealm(),
                userRepresentation = UserRepresentation(
                    firstName = user.firstName,
                    lastName = user.lastName,
                    email = user.email,
                    username = user.email,
                    enabled = true,
                    attributes = mapOf(COMPANY_PROP_NAME to arrayOf(user.companyId.toString()))
                )
            )

            keycloakUser = keycloakAdminClient.getUsersApi().realmUsersGet(
                realm = keycloakAdminClient.getRealm(),
                email = user.email
            ).firstOrNull() ?: return null

            assignUserToProjectGroups(keycloakUser, emptyArray(), groupIds)

            // Assign user to USER role
            val userRole = keycloakAdminClient.getRoleContainerApi().realmRolesRoleNameGet(
                roleName = UserRole.USER.NAME,
                realm = keycloakAdminClient.getRealm()
            )
            keycloakAdminClient.getRoleMapperApi().realmUsersIdRoleMappingsRealmPost(
                id = keycloakUser.id.toString(),
                realm = keycloakAdminClient.getRealm(),
                roleRepresentation = arrayOf(userRole)
            )

            userEntity = userRepository.create(
                id = UUID.randomUUID(),
                keycloakId = UUID.fromString(keycloakUser.id)
            )
        } catch (e: Exception) {
            logger.error("Failed to create user:", e)
            return null
        }

        try {
            keycloakAdminClient.getUserApi().realmUsersIdExecuteActionsEmailPut(
                realm = keycloakAdminClient.getRealm(),
                id = keycloakUser.id!!,
                requestBody = arrayOf("UPDATE_PASSWORD"),
                lifespan = null
            )
        } catch (e: Exception) {
            logger.error("Failed sending email to user", e)
            // Delete the user if the email sending fails
            deleteUser(userEntity)
        }

        return UserFullRepresentation(
            userEntity = userEntity!!,
            userRepresentation = keycloakUser
        )
    }

    suspend fun findUser(userId: UUID): UserEntity? {
        return userRepository.findByIdSuspending(userId)
    }

    suspend fun findUserByKeycloakId(keycloakId: UUID): UserEntity? {
        return userRepository.findByKeycloakId(keycloakId)
    }

    /**
     * Finds a user by id
     *
     * @param keycloakId user id
     * @return found user or null if not found
     */
    suspend fun findKeycloakUser(keycloakId: UUID): UserRepresentation? {
        return try {
            keycloakAdminClient.getUserApi().realmUsersIdGet(
                realm = keycloakAdminClient.getRealm(),
                id = keycloakId.toString()
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
     * @param updateGroups new user groups
     *
     * @return updated user
     */
    suspend fun updateUser(
        existingUser: UserEntity,
        updateData: User,
        updateGroups: List<UUID>?
    ): UserFullRepresentation? {
        println("updating user ${existingUser.id} with keycloak id ${existingUser.keycloakId}")
        val currentKeycloakRepresentation = findKeycloakUser(existingUser.keycloakId) ?: return null
        val updatedRepresentation = currentKeycloakRepresentation.copy(
            firstName = updateData.firstName,
            lastName = updateData.lastName,
            attributes = if (updateData.companyId != null) {
                mapOf(COMPANY_PROP_NAME to arrayOf(updateData.companyId.toString()))
            } else {
                emptyMap()
            }
        )
        return try {
            keycloakAdminClient.getUserApi().realmUsersIdPut(
                realm = keycloakAdminClient.getRealm(),
                id = existingUser.keycloakId.toString(),
                userRepresentation = updatedRepresentation
            )

            assignUserToProjectGroups(currentKeycloakRepresentation, currentKeycloakRepresentation.groups, updateGroups)
            UserFullRepresentation(
                userRepresentation = keycloakAdminClient.getUserApi().realmUsersIdGet(
                    realm = keycloakAdminClient.getRealm(),
                    id = existingUser.keycloakId.toString()
                ),
                userEntity = existingUser
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
    suspend fun deleteUser(userEntity: UserEntity) {
        try {
            keycloakAdminClient.getUserApi().realmUsersIdDelete(
                realm = keycloakAdminClient.getRealm(),
                id = userEntity.keycloakId.toString()
            )
            userRepository.deleteSuspending(userEntity)
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

    /**
     * Assigns user to groups
     *
     * @param currentGroups current groups
     * @param updateGroups new groups
     */
    suspend fun assignUserToProjectGroups(
        existingUser: UserRepresentation,
        currentGroups: Array<String>?,
        updateGroups: List<UUID>?
    ) {
        println("Assigning user ${existingUser.id} to groups ${updateGroups?.joinToString()}")
        // if group is present in current but not in update then unassign
        currentGroups?.forEach { currentGroupId ->
            if (updateGroups?.find { it.toString() == currentGroupId } == null) {
                keycloakAdminClient.getUserApi().realmUsersIdGroupsGroupIdDelete(
                    realm = keycloakAdminClient.getRealm(),
                    id = existingUser.id!!,
                    groupId = currentGroupId
                )
            }
        }

        // if group is present in update but not in current then assign
        updateGroups?.forEach { groupId ->
            if (currentGroups?.find { it == groupId.toString() } == null) {
                keycloakAdminClient.getUserApi().realmUsersIdGroupsGroupIdPut(
                    realm = keycloakAdminClient.getRealm(),
                    id = existingUser.id!!,
                    groupId = groupId.toString()
                )
            }
        }
        println("User ${existingUser.id} assigned to groups ${updateGroups?.joinToString()}")
    }

    companion object {
        private const val COMPANY_PROP_NAME = "companyId"
    }
}