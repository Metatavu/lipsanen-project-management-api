package fi.metatavu.lipsanen.projects

import fi.metatavu.lipsanen.api.model.Project
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*

/**
 * Controller for projects
 */
@ApplicationScoped
class ProjectController {

    @Inject
    lateinit var projectRepository: ProjectRepository

    /**
     * Creates a new project
     *
     * @param project project
     * @param userId user id
     * @return created project
     */
    suspend fun createProject(project: Project, userId: UUID): ProjectEntity {
        return projectRepository.create(
            name = project.name,
            tocomanId = project.tocomanId,
            creatorId = userId
        )
    }

    /**
     * Creates a new project
     *
     * @param name project name
     * @param tocomanId tocoman id
     * @param userId user id
     * @return created project
     */
    suspend fun createProject(name: String, tocomanId: Int, userId: UUID): ProjectEntity {
        return projectRepository.create(
            name = name,
            tocomanId = tocomanId,
            creatorId = userId
        )
    }

    /**
     * Finds a project by id
     *
     * @param id project id
     * @return found project or null if not found
     */
    suspend fun findProject(id: UUID): ProjectEntity? {
        return projectRepository.findByIdSuspending(id)
    }

    /**
     * Finds a project by tocoman id
     *
     * @param tocomanId tocoman id
     * @return found project or null if not found
     */
    suspend fun findProjectByTocomanId(tocomanId: Int): ProjectEntity? {
        return projectRepository.find("tocomanId", tocomanId).firstResult<ProjectEntity>().awaitSuspending()
    }

    /**
     * Lists all projects
     *
     * @return list of projects
     */
    suspend fun listProjects(): Pair<List<ProjectEntity>, Long> {
        return projectRepository.listAllSuspending(null, null)
    }

    /**
     * Updates a project
     *
     * @param existingProject existing project
     * @param project project
     * @param userId user id
     * @return updated project
     */
    suspend fun updateProject(existingProject: ProjectEntity, project: Project, userId: UUID): ProjectEntity {
        existingProject.name = project.name
        existingProject.tocomanId = project.tocomanId
        existingProject.lastModifierId = userId
        return projectRepository.persistSuspending(existingProject)
    }

    /**
     * Updates a project
     *
     * @param existingProject existing project
     * @param name project name
     * @param userId user id
     * @return updated project
     */
    suspend fun updateProject(existingProject: ProjectEntity, name: String, userId: UUID): ProjectEntity {
        existingProject.name = name
        existingProject.lastModifierId = userId
        return projectRepository.persistSuspending(existingProject)
    }

    /**
     * Deletes a project
     *
     * @param projectEntity project entity
     */
    suspend fun deleteProject(projectEntity: ProjectEntity) {
        projectRepository.deleteSuspending(projectEntity)
    }
}