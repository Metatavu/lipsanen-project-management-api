package fi.metatavu.lipsanen.projects

import fi.metatavu.lipsanen.api.model.Project
import fi.metatavu.lipsanen.api.spec.ProjectsApi
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
import fi.metatavu.lipsanen.tocoman.TocomanController
import fi.metatavu.lipsanen.users.UserController
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.vertx.core.Vertx
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File
import java.util.*

/**
 * Projects API implementation
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RequestScoped
@WithSession
class ProjectsApiImpl : ProjectsApi, AbstractApi() {

    @Inject
    lateinit var projectTranslator: ProjectTranslator

    @Inject
    lateinit var tocomanController: TocomanController

    @Inject
    lateinit var userController: UserController

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun listProjects(first: Int?, max: Int?): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        val (projects, count) = if (isAdmin()) {
             projectController.listProjects(first, max)
        } else {
            val user = userController.findUser(userId) ?: return@withCoroutineScope createInternalServerError("Failed to find a user")
            projectController.listProjectsForUser(user, first, max)
        }
        return@withCoroutineScope createOk(projects.map { projectTranslator.translate(it) }, count)
    }

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun createProject(project: Project): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        if (projectController.findProjectByName(project.name) != null) return@withCoroutineScope createConflict("Project with given name already exists!")
        val created = projectController.createProject(project, userId) ?: return@withCoroutineScope createInternalServerError("Failed to create a project")
        return@withCoroutineScope createOk(projectTranslator.translate(created))
    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun findProject(projectId: UUID): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        val ( project, error ) = getProjectAccessRights(projectId, userId)
        error?.let { return@withCoroutineScope error }

        return@withCoroutineScope createOk(projectTranslator.translate(project!!))
    }

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun updateProject(projectId: UUID, project: Project): Uni<Response> =
        withCoroutineScope {
            val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
            val ( existingProject, error ) = getProjectAccessRights(projectId, userId)
            error?.let { return@withCoroutineScope error }

            val updated = projectController.updateProject(existingProject!!, project, userId) ?: return@withCoroutineScope createInternalServerError(
                "Failed to update a project"
            )
            return@withCoroutineScope createOk(projectTranslator.translate(updated))
        }

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun deleteProject(projectId: UUID): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        val ( existingProject, error ) = getProjectAccessRights(projectId, userId)
        error?.let { return@withCoroutineScope error }

        projectController.deleteProject(existingProject!!)
        return@withCoroutineScope createNoContent()
    }

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun importXml(body: File?): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        if (body == null) {
            return@withCoroutineScope createBadRequest(MISSING_REQUEST_BODY)
        }
        val imported = tocomanController.importProjects(body, userId) ?: return@withCoroutineScope createBadRequest("Failed to import a project")
        createOk(projectTranslator.translate(imported))
    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun exportProject(projectId: UUID): Uni<Response> = withCoroutineScope {
        val userId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        val ( existingProject, error ) = getProjectAccessRights(projectId, userId)
        error?.let { return@withCoroutineScope error }

        val stream = tocomanController.exportProject(existingProject!!)
        return@withCoroutineScope Response.ok(stream)
            .header("Content-Type", "application/xml")
            .build();
    }

}