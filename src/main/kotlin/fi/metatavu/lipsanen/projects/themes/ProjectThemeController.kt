package fi.metatavu.lipsanen.projects.themes

import fi.metatavu.lipsanen.api.model.ProjectTheme
import fi.metatavu.lipsanen.projects.ProjectEntity
import io.quarkus.panache.common.Parameters
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

/**
 * Controller for project themes
 */
@ApplicationScoped
class ProjectThemeController {

    @Inject
    lateinit var projectThemeRepository: ProjectThemeRepository

    /**
     * Lists project themes
     *
     * @param project project
     * @return list of project themes
     */
    suspend fun list(project: ProjectEntity): Pair<List<ProjectThemeEntity>, Long> {
        return projectThemeRepository.list(project)
    }

    /**
     * Creates a new project theme
     *
     * @param project project
     * @param projectTheme project theme
     * @param userId user id
     * @return created project theme
     */
    suspend fun create(project: ProjectEntity, projectTheme: ProjectTheme, userId: UUID): ProjectThemeEntity {
        return projectThemeRepository.create(
            id = UUID.randomUUID(),
            themeColor = projectTheme.themeColor,
            logoUrl = projectTheme.logoUrl,
            project = project,
            creatorId = userId,
            lastModifierId = userId
        )
    }

    /**
     * Finds project theme by id
     *
     * @param project project
     * @param themeId theme id
     * @return found project theme or null if not found
     */
    suspend fun find(project: ProjectEntity, themeId: UUID): ProjectThemeEntity? {
        return projectThemeRepository.find(
            "project = :project and id = :id",
            Parameters.with("project", project).and("id", themeId)
        ).firstResult<ProjectThemeEntity>().awaitSuspending()
    }

    /**
     * Updates project theme
     *
     * @param existingTheme existing theme
     * @param projectTheme project theme
     * @param userId user id
     * @return updated project theme
     */
    suspend fun update(
        existingTheme: ProjectThemeEntity,
        projectTheme: ProjectTheme,
        userId: UUID
    ): ProjectThemeEntity {
        existingTheme.themeColor = projectTheme.themeColor
        existingTheme.logoUrl = projectTheme.logoUrl
        existingTheme.lastModifierId = userId
        return projectThemeRepository.persistSuspending(existingTheme)
    }

    /**
     * Deletes project theme
     *
     * @param existingTheme existing theme
     */
    suspend fun delete(existingTheme: ProjectThemeEntity) {
        projectThemeRepository.deleteSuspending(existingTheme)
    }
}