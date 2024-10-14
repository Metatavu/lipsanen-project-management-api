package fi.metatavu.lipsanen.users

import fi.metatavu.keycloak.adminclient.models.*
import fi.metatavu.lipsanen.api.model.User
import fi.metatavu.lipsanen.api.model.UserRole
import fi.metatavu.lipsanen.companies.CompanyEntity
import fi.metatavu.lipsanen.keycloak.KeycloakAdminClient
import fi.metatavu.lipsanen.notifications.notificationevents.NotificationEventsController
import fi.metatavu.lipsanen.positions.JobPositionEntity
import fi.metatavu.lipsanen.projects.ProjectEntity
import fi.metatavu.lipsanen.tasks.comments.TaskCommentUserRepository
import fi.metatavu.lipsanen.users.userstoprojects.UserToProjectEntity
import fi.metatavu.lipsanen.users.userstoprojects.UserToProjectRepository
import io.quarkus.hibernate.reactive.panache.Panache
import io.smallrye.mutiny.coroutines.awaitSuspending
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
    lateinit var notificationEventsController: NotificationEventsController

    @Inject
    lateinit var taskCommentUserRepository: TaskCommentUserRepository

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
     * @param company company
     * @param projectFilter project
     * @param jobPosition job position
     * @param first first result
     * @param max max results
     * @return users
     */
    suspend fun listUsers(
        company: CompanyEntity?,
        projectFilter: List<ProjectEntity>?,
        jobPosition: JobPositionEntity?,
        first: Int?,
        max: Int?
    ): Pair<List<UserFullRepresentation>, Long> {
        val (userEntities, entitiesCount) = userRepository.list(
            companyEntity = company,
            project = projectFilter,
            jobPosition = jobPosition,
            firstResult =  first,
            maxResults = max
        )
        val userRepresentations = userEntities.map {
            val userRepresentation =
                keycloakAdminClient.findUserById(UUID.fromString(it.id.toString()))

            if (userRepresentation == null) {
                logger.error("User ${it.id} not found in keycloak")
                return@map null
            }


            UserFullRepresentation(
                userEntity = it,
                userRepresentation = userRepresentation
            )
        }.filterNotNull()

        return userRepresentations to entitiesCount
    }

    /**
     * Lists users
     *
     * @param companyEntity company entity
     * @param jobPosition job position
     * @return array of users
     */
    suspend fun listUserEntities(companyEntity: CompanyEntity? = null, jobPosition: JobPositionEntity? = null): Pair<List<UserEntity>, Long> {
        return userRepository.list(companyEntity, jobPosition, null, null)
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
     * @param company company
     * @param jobPosition job position
     * @param projects projects
     * @return created user
     */
    suspend fun createUser(
        user: User,
        company: CompanyEntity?,
        jobPosition: JobPositionEntity?,
        projects: List<ProjectEntity>?
    ): UserFullRepresentation? {
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

            val assignableRoles = if (user.roles.isNullOrEmpty()) {
                listOf(UserRole.USER)
            } else {
                user.roles
            }
            assignRoles(keycloakUser, assignableRoles)

            val userEntity = userRepository.create(
                id = UUID.fromString(keycloakUser.id),
                company = company,
                jobPosition = jobPosition
            )
            assignUserToProjects(userEntity, projects ?: emptyList())

            // Update keycloak data with the internal user id
            keycloakAdminClient.getUserApi().realmUsersIdPut(
                realm = keycloakAdminClient.getRealm(),
                id = keycloakUser.id!!,
                userRepresentation = keycloakUser.copy(
                    attributes = mapOf("userId" to arrayOf(userEntity.id.toString()))
                )
            )

            if (environment.orElse(null) != "test") {
                keycloakAdminClient.getUserApi().realmUsersIdExecuteActionsEmailPut(
                    realm = keycloakAdminClient.getRealm(),
                    id = keycloakUser.id,
                    requestBody = arrayOf("UPDATE_PASSWORD"),
                    lifespan = null
                )
            }

            UserFullRepresentation(
                userEntity = userEntity,
                userRepresentation = findKeycloakUser(userEntity.id)!!
            )
        }.getOrElse {
            Panache.currentTransaction().awaitSuspending().markForRollback()
            logger.error("Failed to create user:", it)
            null
        }
    }

    /**
     * Persists user entity
     *
     * @param userEntity user entity
     * @return persisted user entity
     */
    suspend fun persistUserEntity(userEntity: UserEntity): UserEntity {
        return userRepository.persistSuspending(userEntity)
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
     * Finds a user by id
     *
     * @param id user id
     * @return found user or null if not found
     */
    suspend fun findKeycloakUser(id: UUID): UserRepresentation? {
        return try {
            keycloakAdminClient.getUserApi().realmUsersIdGet(
                realm = keycloakAdminClient.getRealm(),
                id = id.toString()
            )
        } catch (e: Exception) {
            logger.error("Failed to find user", e)
            null
        }
    }

    /**
     * Updates a user completely (but not its email)
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
        projects: List<ProjectEntity>?,
        jobPosition: JobPositionEntity?
    ): UserFullRepresentation? {
        val currentKeycloakRepresentation = findKeycloakUser(existingUser.id) ?: return null
        val updatedRepresentation = currentKeycloakRepresentation.copy(
            firstName = updateData.firstName,
            lastName = updateData.lastName,
            realmRoles = updateData.roles?.map { translateRole(it) }?.toTypedArray(),
        )
        return try {
            keycloakAdminClient.getUserApi().realmUsersIdPut(
                realm = keycloakAdminClient.getRealm(),
                id = existingUser.id.toString(),
                userRepresentation = updatedRepresentation
            )

            existingUser.company = company
            existingUser.jobPosition = jobPosition
            userRepository.persistSuspending(existingUser)

            // Assign user to selected roles
            assignRoles(currentKeycloakRepresentation, updateData.roles)

            assignUserToProjects(existingUser, projects ?: emptyList())
            UserFullRepresentation(
                userRepresentation = findKeycloakUser(existingUser.id)!!,
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
    suspend fun assignRoles(userRepresentation: UserRepresentation, roles: List<UserRole>?) {
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
     * Deletes a user and dependencies
     *
     * @param userEntity user entity
     */
    suspend fun deleteUser(userEntity: UserEntity) {
        try {
            keycloakAdminClient.getUserApi().realmUsersIdDelete(
                realm = keycloakAdminClient.getRealm(),
                id = userEntity.id.toString()
            )
            userToProjectRepository.list(userEntity).forEach {
                userToProjectRepository.deleteSuspending(it)
            }
            notificationEventsController.list(receiver = userEntity).first.forEach {
                notificationEventsController.delete(it)
            }
            taskCommentUserRepository.list(userEntity).forEach {
                taskCommentUserRepository.deleteSuspending(it)
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
     * Lists user projects
     *
     * @param user user
     * @return list of user projects
     */
    suspend fun listUserProjects(
        user: UserEntity,
    ): List<UserToProjectEntity> {
        return userToProjectRepository.list(user)
    }

    /**
     * Translates user role to keycloak
     *
     * @param userRole user role
     * @return translated role
     */
    private fun translateRole(userRole: UserRole): String {
        return when (userRole) {
            UserRole.ADMIN -> "admin"
            UserRole.USER -> "user"
            UserRole.PROJECT_OWNER -> "project-owner"
        }
    }
}
