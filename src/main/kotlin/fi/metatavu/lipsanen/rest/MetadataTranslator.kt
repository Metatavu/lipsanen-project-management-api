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
        return fi.metatavu.lipsanen.api.model.Metadata(
            creatorId = entity.creatorId,
            lastModifierId = entity.lastModifierId,
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt
        )
    }

}