package fi.metatavu.lipsanen.attachments

import fi.metatavu.lipsanen.api.model.Attachment
import fi.metatavu.lipsanen.rest.AbstractTranslator
import fi.metatavu.lipsanen.rest.MetadataTranslator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Translator for attachments
 */
@ApplicationScoped
class AttachmentTranslator : AbstractTranslator<AttachmentEntity, Attachment>() {

    @Inject
    lateinit var metadataTranslator: MetadataTranslator

    override suspend fun translate(entity: AttachmentEntity): Attachment {
        return Attachment(
            id = entity.id,
            url = entity.url,
            type = entity.attachmentType,
            name = entity.name,
            projectId = entity.project.id,
            taskId = entity.task?.id,
            metadata = metadataTranslator.translate(entity)
        )
    }
}