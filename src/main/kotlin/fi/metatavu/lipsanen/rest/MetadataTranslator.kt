package fi.metatavu.lipsanen.rest

import fi.metatavu.lipsanen.persistence.Metadata
import jakarta.enterprise.context.ApplicationScoped

/**
 * Translator for metadata
 */
@ApplicationScoped
class MetadataTranslator :
    AbstractTranslator<Metadata, fi.metatavu.lipsanen.api.model.Metadata>() {
    override suspend fun translate(entity: Metadata): fi.metatavu.lipsanen.api.model.Metadata {
        return fi.metatavu.lipsanen.api.model.Metadata(
            creatorId = entity.creatorId,
            lastModifierId = entity.lastModifierId,
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt
        )
    }

}