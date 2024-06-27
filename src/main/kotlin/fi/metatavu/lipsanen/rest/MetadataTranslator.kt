package fi.metatavu.lipsanen.rest

import fi.metatavu.lipsanen.persistence.Metadata
import fi.metatavu.lipsanen.users.UserController
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Translator for metadata
 */
@ApplicationScoped
class MetadataTranslator :
    AbstractTranslator<Metadata, fi.metatavu.lipsanen.api.model.Metadata>() {

    @Inject
    lateinit var userController: UserController

    override suspend fun translate(entity: Metadata): fi.metatavu.lipsanen.api.model.Metadata {
        val creatorId = userController.findUserByKeycloakId(entity.creatorId)?.id
        val modifierId = if (entity.creatorId == entity.lastModifierId) {
            creatorId
        } else {
            userController.findUserByKeycloakId(entity.lastModifierId)?.id
        }
        return fi.metatavu.lipsanen.api.model.Metadata(
            creatorId = creatorId,
            lastModifierId = modifierId,
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt
        )
    }

}