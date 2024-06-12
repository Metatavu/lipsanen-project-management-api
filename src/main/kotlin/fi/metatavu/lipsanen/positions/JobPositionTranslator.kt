package fi.metatavu.lipsanen.positions

import fi.metatavu.lipsanen.api.model.JobPosition
import fi.metatavu.lipsanen.rest.AbstractTranslator
import fi.metatavu.lipsanen.rest.MetadataTranslator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class JobPositionTranslator: AbstractTranslator<JobPositionEntity, JobPosition>(){

    @Inject
    lateinit var metadataTranslator: MetadataTranslator

    override suspend fun translate(entity: JobPositionEntity): JobPosition {
        return JobPosition(
            id = entity.id,
            name = entity.name,
            color = entity.color,
            iconName = entity.iconName,
            metadata = metadataTranslator.translate(entity)
        )
    }
}