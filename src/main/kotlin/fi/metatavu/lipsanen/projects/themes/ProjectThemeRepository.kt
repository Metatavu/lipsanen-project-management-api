package fi.metatavu.lipsanen.projects.themes

import fi.metatavu.lipsanen.persistence.AbstractRepository
import fi.metatavu.lipsanen.projects.ProjectEntity
import io.quarkus.panache.common.Parameters
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Project theme repository
 */
@ApplicationScoped
class ProjectThemeRepository : AbstractRepository<ProjectThemeEntity, UUID>() {

    /**
     * Creates a new project theme
     *
     * @param id id
     * @param themeColor theme color
     * @param logoUrl logo url
     * @param project project
     * @param creatorId creator id
     * @param lastModifierId last modifier id
     * @return created project theme
     */
    suspend fun create(
        id: UUID,
        themeColor: String,
        logoUrl: String,
        project: ProjectEntity,
        creatorId: UUID,
        lastModifierId: UUID
    ): ProjectThemeEntity {
        val projectThemeEntity = ProjectThemeEntity()
        projectThemeEntity.id = id
        projectThemeEntity.themeColor = themeColor
        projectThemeEntity.logoUrl = logoUrl
        projectThemeEntity.project = project
        projectThemeEntity.creatorId = creatorId
        projectThemeEntity.lastModifierId = lastModifierId
        return persistSuspending(projectThemeEntity)
    }

    /**
     * Lists project themes
     *
     * @param project project
     * @return list of project themes
     */
    suspend fun list(
        project: ProjectEntity
    ): Pair<List<ProjectThemeEntity>, Long> {
        val sb = "project = :project"
        val parameters = Parameters()
        parameters.and("project", project)

        sb.plus(" order by modifiedAt desc")
        return applyFirstMaxToQuery(
            query = find(sb, parameters),
            firstIndex = null,
            maxResults = null
        )
    }

}