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
class UserTranslator : AbstractTranslator<UserFullRepresentation, User>() {

    @Inject
    lateinit var userController: UserController

    @Inject
    lateinit var projectController: ProjectController

    override suspend fun translate(entity: UserFullRepresentation): User {
        println("UserTranslator.translate()")
        val userRepresentation = entity.userRepresentation
        val userGroups = userController.listUserGroups(UUID.fromString(userRepresentation.id))
        val projects = projectController.listProjects(keycloakGroupIds = userGroups.map { UUID.fromString(it.id) }, null, null)
        val lastEvent = userController.getLastLogin(UUID.fromString(userRepresentation.id))
        val company = userRepresentation.attributes?.get("companyId")?.firstOrNull()
        return User(
            id = entity.userEntity.id,
            email = userRepresentation.email ?: "",
            firstName = userRepresentation.firstName ?: "",
            lastName = userRepresentation.lastName ?: "",
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
    suspend fun translate(entity: UserFullRepresentation, includeRoles: Boolean?): User {
        return if (includeRoles == true) {
            val roles = userController.getUserRealmRoles(entity.userEntity!!.keycloakId).mapNotNull {
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
data class UserFullRepresentation(
    val userEntity: UserEntity,
    val userRepresentation: UserRepresentation
)