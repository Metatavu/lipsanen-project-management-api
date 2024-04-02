package fi.metatavu.lipsanen.projects

import fi.metatavu.keycloak.adminclient.models.GroupRepresentation
import fi.metatavu.lipsanen.api.model.Project
import fi.metatavu.lipsanen.keycloak.KeycloakAdminClient
import fi.metatavu.lipsanen.users.UserController
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
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
    lateinit var keycloakAdminClient: KeycloakAdminClient

    @Inject
    lateinit var logger: Logger

    /**
     * Creates a new project group and joins a user to it
     *
     * @param projectName project name
     * @param userId creator id
     * @return created group or null if failed
     */
    suspend fun createProjectGroupJoin(projectName: String, userId: UUID): GroupRepresentation? {
        return try {
            keycloakAdminClient.getGroupsApi().realmGroupsPost(
                realm = keycloakAdminClient.getRealm(),
                groupRepresentation = GroupRepresentation(
                    name = projectName
                )
            )

            val createdGroup = keycloakAdminClient.getGroupsApi().realmGroupsGet(
                realm = keycloakAdminClient.getRealm(),
                search = projectName,
                briefRepresentation = true
            ).firstOrNull()

            keycloakAdminClient.getUserApi().realmUsersIdGroupsGroupIdPut(
                realm = keycloakAdminClient.getRealm(),
                id = userId.toString(),
                groupId = createdGroup?.id.toString()
            )
            createdGroup
        } catch (e: Exception) {
            logger.error("Failed to create group $projectName", e)
            null
        }
    }

    /**
     * Creates a new project
     *
     * @param project project
     * @param userId user id
     * @return created project
     */
    suspend fun createProject(project: Project, userId: UUID): ProjectEntity? {
        val createdGroup = createProjectGroupJoin(project.name, userId) ?: return null
        return projectRepository.create(
            name = project.name,
            tocomanId = project.tocomanId,
            keycloakGroupId = createdGroup.id!!,
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
    suspend fun createProject(name: String, tocomanId: Int, userId: UUID): ProjectEntity? {
        val createdGroup = createProjectGroupJoin(name, userId) ?: return null
        return projectRepository.create(
            name = name,
            tocomanId = tocomanId,
            creatorId = userId,
            keycloakGroupId = createdGroup.id!!
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
     * Lists all projects
     *
     * @return list of projects
     */
    suspend fun listProjects(keycloakGroupIds: List<UUID>?, first: Int?, max: Int?): Pair<List<ProjectEntity>, Long> {
        return projectRepository.list(keycloakGroupIds, first, max)
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
        if (existingProject.name != project.name) {
            try {
                keycloakAdminClient.getGroupApi().realmGroupsIdPut(
                    realm = keycloakAdminClient.getRealm(),
                    id = existingProject.keycloakGroupId.toString(),
                    groupRepresentation = GroupRepresentation(
                        name = project.name
                    )
                )
            } catch (e: Exception) {
                logger.error("Failed to update group ${project.name}", e)
                return null
            }
        }
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
     * Deletes a project from keycloak and database
     *
     * @param projectEntity project entity
     */
    suspend fun deleteProject(projectEntity: ProjectEntity): Int? {
        try {
            keycloakAdminClient.getGroupApi().realmGroupsIdDelete(
                realm = keycloakAdminClient.getRealm(),
                id = projectEntity.keycloakGroupId.toString()
            )
        } catch (e: Exception) {
            logger.error("Failed to delete group ${projectEntity.keycloakGroupId}", e)
            return 1
        }
        projectRepository.deleteSuspending(projectEntity)
        return null
    }

    /**
     * Checks if a user has access to a project
     *
     * @param project project
     * @param userId user id
     * @return true if user has access to the project; false otherwise
     */
    suspend fun hasAccessToProject(project: ProjectEntity, userId: UUID): Boolean {
        return userController.listUserGroups(userId).any { it.id == project.keycloakGroupId.toString() }
    }
}