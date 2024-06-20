package fi.metatavu.lipsanen.users

import fi.metatavu.keycloak.adminclient.models.*
import fi.metatavu.lipsanen.api.model.User
import fi.metatavu.lipsanen.companies.CompanyEntity
import fi.metatavu.lipsanen.keycloak.KeycloakAdminClient
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.users.userstoprojects.UserToProjectRepository
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
    lateinit var userToProjectRepository: UserToProjectRepository

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
    suspend fun listUsers(company: CompanyEntity?, first: Int?, max: Int?): Pair<List<UserFullRepresentation>, Long> {
        val (userEntities, entitiesCount) = userRepository.list(company, first, max)
        var totalCount = entitiesCount
        val userReprensentations = userEntities.map {
            val userRepresentation = keycloakAdminClient.findUserById(UUID.fromString(it.keycloakId.toString()))
            if (userRepresentation?.username == keycloakAdminUser) {
                totalCount -= 1
            }

            if (userRepresentation == null) {
                return@map null
            }

            UserFullRepresentation(
                userEntity = it,
                userRepresentation = userRepresentation
            )
        }.filterNotNull()

        return userReprensentations to totalCount
    }

    /**
     * Lists users
     *
     * @param companyId company id
     * @return array of users
     */
    suspend fun listUsers(companyEntity: CompanyEntity): Pair<List<UserEntity>, Long> {
        return userRepository.list(companyEntity, null, null)
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
    suspend fun createUser(user: User, company: CompanyEntity?, projects: List<ProjectEntity>?): UserFullRepresentation? {
        return runCatching {
            val userRepresentation = createUserRepresentation(user)
            keycloakAdminClient.getUsersApi().realmUsersPost(
                realm = keycloakAdminClient.getRealm(),
                userRepresentation = userRepresentation
            )

            val keycloakUser = keycloakAdminClient.getUsersApi().realmUsersGet(
                realm = keycloakAdminClient.getRealm(),
                email = user.email
            ).firstOrNull() ?: return null

            assignRoles(keycloakUser, user.roles)

            val userEntity = userRepository.create(
                id = UUID.randomUUID(),
                keycloakId = UUID.fromString(keycloakUser.id),
                company = company
            )
            assignUserToProjects(userEntity, projects ?: emptyList())

            if (environment.orElse(null) != "test") {
                keycloakAdminClient.getUserApi().realmUsersIdExecuteActionsEmailPut(
                    realm = keycloakAdminClient.getRealm(),
                    id = keycloakUser.id!!,
                    requestBody = arrayOf("UPDATE_PASSWORD"),
                    lifespan = null
                )
            }

            UserFullRepresentation(
                userEntity = userEntity,
                userRepresentation = findKeycloakUser(userEntity.keycloakId)!!
            )
        }.getOrElse {
            logger.error("Failed to create user:", it)
            null
        }
    }

    /**
     * Creates a new user representation
     *
     * @param user user
     * @return created user representation
     */
    private fun createUserRepresentation(user: User): UserRepresentation {
        return UserRepresentation(
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            username = user.email,
            enabled = true,
            credentials = if (environment.orElse(null) == "test") arrayOf(
                CredentialRepresentation(
                    type = "password",
                    temporary = false,
                    value = "test"
                )
            ) else null
        )
    }

    /**
     * Finds a user by id
     *
     * @param userId user id
     * @return found user or null if not found
     */
    suspend fun findUser(userId: UUID): UserEntity? {
        return userRepository.findByIdSuspending(userId)
    }

    /**
     * Finds a user by keycloak id
     *
     * @param keycloakId user id
     * @return found user or null if not found
     */
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
     * @param existingUser existing user
     * @param updateData user
     * @param company company
     * @param projects projects
     * @return updated user
     */
    suspend fun updateUser(
        existingUser: UserEntity,
        updateData: User,
        company: CompanyEntity?,
        projects: List<ProjectEntity>?
    ): UserFullRepresentation? {
        val currentKeycloakRepresentation = findKeycloakUser(existingUser.keycloakId) ?: return null
        val updatedRepresentation = currentKeycloakRepresentation.copy(
            firstName = updateData.firstName,
            lastName = updateData.lastName,
            realmRoles = updateData.roles?.map { translateRole(it) }?.toTypedArray(),
        )
        return try {
            keycloakAdminClient.getUserApi().realmUsersIdPut(
                realm = keycloakAdminClient.getRealm(),
                id = existingUser.keycloakId.toString(),
                userRepresentation = updatedRepresentation
            )

            existingUser.company = company
            userRepository.persistSuspending(existingUser)

            // Assign user to selected roles
            assignRoles(currentKeycloakRepresentation, updateData.roles)

            assignUserToProjects(existingUser, projects ?: emptyList())
            UserFullRepresentation(
                userRepresentation = findKeycloakUser(existingUser.keycloakId)!!,
                userEntity = existingUser
            )
        } catch (e: Exception) {
            logger.error("Failed to update user", e)
            null
        }
    }

    /**
     * Update user roles
     *
     * @param userRepresentation user representation
     * @param roles roles to assign
     */
    suspend fun assignRoles(userRepresentation: UserRepresentation, roles: List<fi.metatavu.lipsanen.api.model.UserRole>?) {
        if (roles == null) {
            //nothing is being updated if roles are null
            return
        }
        val userRoles = roles.map {
            keycloakAdminClient.getRoleContainerApi().realmRolesRoleNameGet(
                roleName = translateRole(it),
                realm = keycloakAdminClient.getRealm()
            )
        }
        val assignedRoles = keycloakAdminClient.getRoleMapperApi().realmUsersIdRoleMappingsGet(
            id = userRepresentation.id.toString(),
            realm = keycloakAdminClient.getRealm(),
        ).realmMappings

        val rolesToUnassign = assignedRoles?.filter { role -> userRoles.find { it.name == role.name } == null }
        val rolesToAssign = userRoles.filter { role -> assignedRoles?.find { it.name == role.name } == null }

        keycloakAdminClient.getRoleMapperApi().realmUsersIdRoleMappingsRealmDelete(
            id = userRepresentation.id.toString(),
            realm = keycloakAdminClient.getRealm(),
            roleRepresentation = rolesToUnassign?.toTypedArray()
        )


        keycloakAdminClient.getRoleMapperApi().realmUsersIdRoleMappingsRealmPost(
            id = userRepresentation.id.toString(),
            realm = keycloakAdminClient.getRealm(),
            roleRepresentation = rolesToAssign.toTypedArray()
        )
    }
    /**
     * Deletes a user
     *
     * @param userEntity user entity
     */
    suspend fun deleteUser(userEntity: UserEntity) {
        try {
            keycloakAdminClient.getUserApi().realmUsersIdDelete(
                realm = keycloakAdminClient.getRealm(),
                id = userEntity.keycloakId.toString()
            )
            userToProjectRepository.list(userEntity).forEach {
                userToProjectRepository.deleteSuspending(it)
            }
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
     * Assigns user to projects
     *
     * @param user user
     * @param newProjects projects
     */
    suspend fun assignUserToProjects(
        user: UserEntity,
        newProjects: List<ProjectEntity>
    ) {
        val currentProject = userToProjectRepository.list(user)

        currentProject.forEach { project ->
            if (newProjects.find { it.id == project.project.id } == null) {
                userToProjectRepository.deleteSuspending(project)
            }
        }
        newProjects.forEach { project ->
            if (currentProject.find { it.project.id == project.id } == null) {
                userToProjectRepository.create(UUID.randomUUID(), user, project)
            }
        }

    }

    /**
     * Translates user role to keycloak
     *
     * @param userRole user role
     * @return translated role
     */
    private fun translateRole(userRole: fi.metatavu.lipsanen.api.model.UserRole): String {
        return when (userRole) {
            fi.metatavu.lipsanen.api.model.UserRole.ADMIN -> "admin"
            fi.metatavu.lipsanen.api.model.UserRole.USER -> "user"
        }
    }
}