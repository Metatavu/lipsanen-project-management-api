package fi.metatavu.lipsanen.projects

import fi.metatavu.lipsanen.api.model.Project
import fi.metatavu.lipsanen.api.model.ProjectStatus
import fi.metatavu.lipsanen.rest.AbstractTranslator
import fi.metatavu.lipsanen.rest.MetadataTranslator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Translator for projects
 */
@ApplicationScoped
class ProjectTranslator : AbstractTranslator<ProjectEntity, Project>() {

    @Inject
    lateinit var metadataTranslator: MetadataTranslator

    override suspend fun translate(entity: ProjectEntity): Project {
        println(entity.id)
        return Project(
            id = entity.id,
            name = entity.name,
            tocomanId = entity.tocomanId,
            status = entity.status,
            metadata = metadataTranslator.translate(entity)
        )
    }
}