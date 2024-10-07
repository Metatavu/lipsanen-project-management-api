package fi.metatavu.lipsanen.projects

import fi.metatavu.lipsanen.api.model.ProjectStatus
import fi.metatavu.lipsanen.persistence.AbstractRepository
import io.quarkus.panache.common.Parameters
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.*

/**
 * Repository for projects
 */
@ApplicationScoped
class ProjectRepository : AbstractRepository<ProjectEntity, UUID>() {

    /**
     * Creates a new project
     *
     * @param id project id
     * @param name project name
     * @param tocomanId tocoman id
     * @param estimatedStartDate estimated start date
     * @param estimatedEndDate estimated end date
     * @param creatorId creator id
     * @return created project
     */
    suspend fun create(id: UUID, name: String, tocomanId: Int?, estimatedStartDate: LocalDate?, estimatedEndDate: LocalDate?, creatorId: UUID): ProjectEntity {
        val project = ProjectEntity()
        project.id = id
        project.name = name
        project.status = ProjectStatus.INITIATION
        project.tocomanId = tocomanId
        project.estimatedStartDate = estimatedStartDate
        project.estimatedEndDate = estimatedEndDate
        project.creatorId = creatorId
        project.lastModifierId = creatorId
        return persistSuspending(project)
    }

    /**
     * Lists projects
     *
     * @param first first result
     * @param max max results
     * @return list of projects
     */
    suspend fun list(first: Int?, max: Int?): Pair<List<ProjectEntity>, Long> {
        val queryBuilder = StringBuilder()
        val parameters = Parameters()

        return applyFirstMaxToQuery(
            query = find(queryBuilder.toString(), Sort.descending("createdAt"), parameters),
            firstIndex = first,
            maxResults = max
        )
    }
}