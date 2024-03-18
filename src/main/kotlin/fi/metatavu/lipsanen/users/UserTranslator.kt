package fi.metatavu.lipsanen.users

import fi.metatavu.keycloak.adminclient.models.UserRepresentation
import fi.metatavu.lipsanen.api.model.User
import fi.metatavu.lipsanen.rest.AbstractTranslator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@ApplicationScoped
class UserTranslator : AbstractTranslator<UserRepresentation, User>() {

    @Inject
    lateinit var userController: UserController

    override suspend fun translate(entity: UserRepresentation): User {
        val lastEvent = userController.getLastLogin(UUID.fromString(entity.id))
        return User(
            id = UUID.fromString(entity.id),
            email = entity.email ?: "",
            firstName = entity.firstName ?: "",
            lastName = entity.lastName ?: "",
            lastLoggedIn = if (lastEvent?.time == null) null else OffsetDateTime.ofInstant(Instant.ofEpochMilli(lastEvent.time), ZoneOffset.UTC)
        )
    }
}