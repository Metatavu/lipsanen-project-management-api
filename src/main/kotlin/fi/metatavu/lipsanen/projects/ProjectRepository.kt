package fi.metatavu.lipsanen.projects

import fi.metatavu.lipsanen.persistence.AbstractRepository
import jakarta.enterprise.context.ApplicationScoped
import java.util.*

/**
 * Repository for projects
 */
@ApplicationScoped
class ProjectRepository : AbstractRepository<ProjectEntity, UUID>() {

    /**
     * Creates a new project
     *
     * @param name project name
     * @param tocomanId tocoman id
     * @param creatorId creator id
     * @return created project
     */
    suspend fun create(name: String, tocomanId: Int?, creatorId: UUID): ProjectEntity {
        val project = ProjectEntity()
        project.id = UUID.randomUUID()
        project.name = name
        project.tocomanId = tocomanId
        project.creatorId = creatorId
        project.lastModifierId = creatorId
        return persistSuspending(project)
    }
}