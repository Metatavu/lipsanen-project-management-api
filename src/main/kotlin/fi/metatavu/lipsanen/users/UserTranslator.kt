package fi.metatavu.lipsanen.users

import fi.metatavu.keycloak.adminclient.models.UserRepresentation
import fi.metatavu.lipsanen.api.model.User
import fi.metatavu.lipsanen.api.model.UserRole
import fi.metatavu.lipsanen.projects.ProjectController
import fi.metatavu.lipsanen.rest.AbstractTranslator
import fi.metatavu.lipsanen.users.userstoprojects.UserToProjectRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Translates UserRepresentation to User
 */
@ApplicationScoped
class UserTranslator : AbstractTranslator<UserFullRepresentation, User>() {

    @Inject
    lateinit var userController: UserController

    @Inject
    lateinit var projectController: ProjectController

    @Inject
    lateinit var userToProjectRepository: UserToProjectRepository

    override suspend fun translate(entity: UserFullRepresentation): User {
        val userRepresentation = entity.userRepresentation
        val projects = userToProjectRepository.list(entity.userEntity)
        val lastEvent = userController.getLastLogin(UUID.fromString(userRepresentation.id))
        return User(
            id = entity.userEntity.id,
            keycloakId = UUID.fromString(userRepresentation.id),
            email = userRepresentation.email ?: "",
            firstName = userRepresentation.firstName ?: "",
            lastName = userRepresentation.lastName ?: "",
            jobPositionId = entity.userEntity.jobPosition?.id,
            companyId = entity.userEntity.company?.id,
            lastLoggedIn = if (lastEvent?.time == null) null else OffsetDateTime.ofInstant(Instant.ofEpochMilli(lastEvent.time), ZoneOffset.UTC),
            projectIds = projects.map { it.project.id },
            roles = emptyList()
        )
    }

    /**
     * Translates UserRepresentation to User based on the includeRoles parameter
     *
     * @param entity UserRepresentation
     * @param includeRoles include roles
     * @return translated User
     */
    suspend fun translate(entity: UserFullRepresentation, includeRoles: Boolean?): User {
        return if (includeRoles == true) {
            val roles = userController.getUserRealmRoles(entity.userEntity.keycloakId).mapNotNull {
                when (it.name) {
                    "admin" -> UserRole.ADMIN
                    "user" -> UserRole.USER
                    "project-owner" -> UserRole.PROJECT_OWNER
                    else -> null
                }
            }
            translate(entity).copy(roles = roles)
        } else {
            translate(entity)
        }
    }
}

/**
 * User full representation
 *
 * @param userEntity user entity
 * @param userRepresentation user representation
 */
data class UserFullRepresentation(
    val userEntity: UserEntity,
    val userRepresentation: UserRepresentation
)