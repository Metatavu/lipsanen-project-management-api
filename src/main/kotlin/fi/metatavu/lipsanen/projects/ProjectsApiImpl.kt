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
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
    override fun listProjects(first: Int?, max: Int?): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val (projects, count) = if (isAdmin()) {
             projectController.listProjects(first, max)
        } else {
            val user = userController.findUserByKeycloakId(userId) ?: return@async createInternalServerError("Failed to find a user")
            projectController.listProjectsForUser(user, first, max)
        }
        return@async createOk(projects.map { projectTranslator.translate(it) }, count)
    }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun createProject(project: Project): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        if (projectController.findProjectByName(project.name) != null) return@async createConflict("Project with given name already exists!")
        val created = projectController.createProject(project, userId) ?: return@async createInternalServerError("Failed to create a project")
        return@async createOk(projectTranslator.translate(created))
    }.asUni()

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun findProject(projectId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val ( project, error ) = getProjectAccessRights(projectId, userId)
        error?.let { return@async error }

        return@async createOk(projectTranslator.translate(project!!))
    }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun updateProject(projectId: UUID, project: Project): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val ( existingProject, error ) = getProjectAccessRights(projectId, userId)
            error?.let { return@async error }

            val updated = projectController.updateProject(existingProject!!, project, userId) ?: return@async createInternalServerError(
                "Failed to update a project"
            )
            return@async createOk(projectTranslator.translate(updated))
        }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun deleteProject(projectId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val ( existingProject, error ) = getProjectAccessRights(projectId, userId)
        error?.let { return@async error }

        projectController.deleteProject(existingProject!!)
        return@async createNoContent()
    }.asUni()

    @RolesAllowed(UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun importXml(body: File?): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        if (body == null) {
            return@async createBadRequest(MISSING_REQUEST_BODY)
        }
        val imported = tocomanController.importProjects(body, userId) ?: return@async createBadRequest("Failed to import a project")
        createOk(projectTranslator.translate(imported))
    }.asUni()

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun exportProject(projectId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val ( existingProject, error ) = getProjectAccessRights(projectId, userId)
        error?.let { return@async error }

        val stream = tocomanController.exportProject(existingProject!!)
        return@async Response.ok(stream)
            .header("Content-Type", "application/xml")
            .build();
    }.asUni()

}