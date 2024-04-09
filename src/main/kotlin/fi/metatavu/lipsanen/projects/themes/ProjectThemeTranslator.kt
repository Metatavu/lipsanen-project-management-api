package fi.metatavu.lipsanen.projects.themes

import fi.metatavu.lipsanen.api.model.ProjectTheme
import fi.metatavu.lipsanen.rest.AbstractTranslator
import fi.metatavu.lipsanen.rest.MetadataTranslator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

/**
 * Translator for project theme
 */
@ApplicationScoped
class ProjectThemeTranslator : AbstractTranslator<ProjectThemeEntity, ProjectTheme>() {

    @Inject
    lateinit var metadataTranslator: MetadataTranslator

    override suspend fun translate(entity: ProjectThemeEntity): ProjectTheme {
        return ProjectTheme(
            id = entity.id,
            themeColor = entity.themeColor,
            logoUrl = entity.logoUrl,
            metadata = metadataTranslator.translate(entity)
        )
    }
}