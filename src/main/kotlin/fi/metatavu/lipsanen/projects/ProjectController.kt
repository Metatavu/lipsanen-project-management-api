package fi.metatavu.lipsanen.projects

import fi.metatavu.lipsanen.api.model.Project
import fi.metatavu.lipsanen.api.model.ProjectStatus
import fi.metatavu.lipsanen.milestones.MilestoneController
import fi.metatavu.lipsanen.projects.themes.ProjectThemeController
import fi.metatavu.lipsanen.users.UserController
import fi.metatavu.lipsanen.users.UserEntity
import fi.metatavu.lipsanen.users.userstoprojects.UserToProjectRepository
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

    @Inject
    lateinit var userController: UserController

    @Inject
    lateinit var projectThemeController: ProjectThemeController

    @Inject
    lateinit var milestoneController: MilestoneController

    @Inject
    lateinit var userToProjectRepository: UserToProjectRepository

    /**
     * Lists all projects
     *
     * @param first first
     * @param max max
     * @return list of projects
     */
    suspend fun listProjects(first: Int?, max: Int?): Pair<List<ProjectEntity>, Long> {
        return projectRepository.applyFirstMaxToQuery(projectRepository.findAll(), first, max)
    }

    /**
     * Lists projects for a user
     *
     * @param user user
     * @param first first
     * @param max max
     * @return list of projects
     */
    suspend fun listProjectsForUser(user: UserEntity, first: Int? = null, max: Int? = null): Pair<List<ProjectEntity>, Long> {
        val (connections, count) = userToProjectRepository.applyFirstMaxToQuery(
            userToProjectRepository.find("user", user),
            first,
            max
        )
        return connections.map { it.project } to count
    }

    /**
     * Creates a new project and assigns its creator to it
     *
     * @param project project
     * @param creatorId user id
     * @return created project
     */
    suspend fun createProject(project: Project, creatorId: UUID): ProjectEntity? {
        return projectRepository.create(
            id = UUID.randomUUID(),
            name = project.name,
            tocomanId = project.tocomanId,
            creatorId = creatorId
        )
    }

    /**
     * Creates a new project
     *
     * @param name project name
     * @param tocomanId tocoman id
     * @param creatorId user id
     * @return created project
     */
    suspend fun createProject(name: String, tocomanId: Int, creatorId: UUID,): ProjectEntity {
        return projectRepository.create(
            id = UUID.randomUUID(),
            name = name,
            tocomanId = tocomanId,
            creatorId = creatorId,
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
     * Finds a project by name
     *
     * @param name project name
     * @return found project or null if not found
     */
    suspend fun findProjectByName(name: String): ProjectEntity? {
        return projectRepository.find("name", name).firstResult<ProjectEntity?>().awaitSuspending()
    }

    /**
     * Updates a project both in keycloak and database
     *
     * @param existingProject existing project
     * @param project project
     * @param userId user id
     * @return updated project
     */
    suspend fun updateProject(existingProject: ProjectEntity, project: Project, userId: UUID): ProjectEntity? {
        existingProject.name = project.name
        existingProject.status = project.status
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
     * Deletes a project from keycloak and database
     *
     * @param projectEntity project entity
     */
    suspend fun deleteProject(projectEntity: ProjectEntity) {
        userToProjectRepository.list(projectEntity).forEach {
            userToProjectRepository.deleteSuspending(it)
        }
        projectThemeController.list(projectEntity).first.forEach {
            projectThemeController.delete(it)
        }
        milestoneController.list(projectEntity).forEach {
            milestoneController.delete(it)
        }
        projectRepository.deleteSuspending(projectEntity)
    }

    /**
     * Checks if a user has access to a project
     *
     * @param project project
     * @param keycloakUserId user id
     * @return true if user has access to the project; false otherwise
     */
    suspend fun hasAccessToProject(project: ProjectEntity, keycloakUserId: UUID): Boolean {
        val userEntity = userController.findUser(keycloakUserId) ?: return false
        val projects = userToProjectRepository.list(userEntity, project)
        return projects != null
    }

    /**
     * Checks if a project is in planning stage
     *
     * @param project project
     * @return true if project is in planning stage; false otherwise
     */
    suspend fun isInPlanningStage(project: ProjectEntity): Boolean {
        return project.status == ProjectStatus.PLANNING || project.status == ProjectStatus.INITIATION || project.status == ProjectStatus.DESIGN
    }

}