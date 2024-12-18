package fi.metatavu.lipsanen.proposals

import fi.metatavu.lipsanen.api.model.ChangeProposal
import fi.metatavu.lipsanen.rest.AbstractTranslator
import fi.metatavu.lipsanen.rest.MetadataTranslator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Translator for proposals
 */
@ApplicationScoped
class ChangeProposalTranslator : AbstractTranslator<ChangeProposalEntity, ChangeProposal>() {

    @Inject
    lateinit var metadataTranslator: MetadataTranslator

    override suspend fun translate(entity: ChangeProposalEntity): ChangeProposal {
        return ChangeProposal(
            id = entity.id,
            taskId = entity.task.id,
            startDate = entity.startDate,
            endDate = entity.endDate,
            reason = entity.reason,
            comment = entity.comment,
            status = entity.status,
            metadata = metadataTranslator.translate(entity),
        )
    }
}