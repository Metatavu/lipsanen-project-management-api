package fi.metatavu.lipsanen.projects.milestones

import fi.metatavu.lipsanen.api.model.Milestone
import fi.metatavu.lipsanen.rest.AbstractTranslator
import fi.metatavu.lipsanen.rest.MetadataTranslator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Translator for milestones
 */
@ApplicationScoped
class MilestoneTranslator : AbstractTranslator<MilestoneEntity, Milestone>() {

    @Inject
    lateinit var metadataTranslator: MetadataTranslator

    override suspend fun translate(entity: MilestoneEntity): Milestone {
        return Milestone(
            id = entity.id,
            name = entity.name,
            startDate = entity.startDate,
            endDate = entity.endDate,
            originalStartDate = entity.originalStartDate,
            originalEndDate = entity.originalEndDate,
            metadata = metadataTranslator.translate(entity)
        )
    }
}