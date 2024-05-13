package fi.metatavu.lipsanen.users

import fi.metatavu.keycloak.adminclient.models.UserRepresentation
import fi.metatavu.lipsanen.api.model.User
import fi.metatavu.lipsanen.api.model.UserRole
import fi.metatavu.lipsanen.projects.ProjectController
import fi.metatavu.lipsanen.rest.AbstractTranslator
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
class UserTranslator : AbstractTranslator<UserRepresentation, User>() {

    @Inject
    lateinit var userController: UserController

    @Inject
    lateinit var projectController: ProjectController

    override suspend fun translate(entity: UserRepresentation): User {
        val userGroups = userController.listUserGroups(UUID.fromString(entity.id))
        val projects = projectController.listProjects(keycloakGroupIds = userGroups.map { UUID.fromString(it.id) }, null, null)
        val lastEvent = userController.getLastLogin(UUID.fromString(entity.id))
        val company = entity.attributes?.get("companyId")?.firstOrNull()
        return User(
            id = UUID.fromString(entity.id),
            email = entity.email ?: "",
            firstName = entity.firstName ?: "",
            lastName = entity.lastName ?: "",
            companyId = if (company != null && company != "null") UUID.fromString(company) else null,
            lastLoggedIn = if (lastEvent?.time == null) null else OffsetDateTime.ofInstant(Instant.ofEpochMilli(lastEvent.time), ZoneOffset.UTC),
            projectIds = projects.first.map { it.id },
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
    suspend fun translate(entity: UserRepresentation, includeRoles: Boolean?): User {
        return if (includeRoles == true) {
            val roles = userController.getUserRealmRoles(UUID.fromString(entity.id)).mapNotNull {
                when (it.name) {
                    "admin" -> UserRole.ADMIN
                    "user" -> UserRole.USER
                    else -> null
                }
            }
            translate(entity).copy(roles = roles)
        } else {
            translate(entity)
        }
    }
}