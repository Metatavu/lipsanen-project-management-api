package fi.metatavu.lipsanen.projects

import fi.metatavu.lipsanen.api.model.Project
import fi.metatavu.lipsanen.api.spec.ProjectsApi
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.tocoman.TocomanController
import io.quarkus.hibernate.reactive.panache.common.WithSession
import io.quarkus.hibernate.reactive.panache.common.WithTransaction
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.dispatcher
import jakarta.enterprise.context.RequestScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import java.io.File
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@RequestScoped
@WithSession
class ProjectsApiImpl : ProjectsApi, AbstractApi() {

    @Inject
    lateinit var projectController: ProjectController

    @Inject
    lateinit var projectTranslator: ProjectTranslator

    @Inject
    lateinit var tocomanController: TocomanController

    @Inject
    lateinit var vertx: Vertx

    override fun listProjects(): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val (projects, count) = projectController.listProjects()
        return@async createOk(projects.map { projectTranslator.translate(it) }, count)
    }.asUni()

    @WithTransaction
    override fun createProject(project: Project): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val created = projectController.createProject(project, userId)
        return@async createOk(projectTranslator.translate(created))
    }.asUni()

    override fun findProject(projectId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val existingProject = projectController.findProject(projectId) ?: return@async createNotFound(
            createNotFoundMessage(
                PROJECT,
                projectId
            )
        )
        return@async createOk(projectTranslator.translate(existingProject))
    }.asUni()

    @WithTransaction
    override fun updateProject(projectId: UUID, project: Project): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val existingProject = projectController.findProject(projectId) ?: return@async createNotFound(
                createNotFoundMessage(
                    PROJECT,
                    projectId
                )
            )
            val updated = projectController.updateProject(existingProject, project, userId)
            return@async createOk(projectTranslator.translate(updated))
        }.asUni()

    @WithTransaction
    override fun deleteProject(projectId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val existingProject = projectController.findProject(projectId) ?: return@async createNotFound(
            createNotFoundMessage(
                PROJECT,
                projectId
            )
        )
        projectController.deleteProject(existingProject)
        return@async createNoContent()
    }.asUni()


    @WithTransaction
    override fun importXml(body: File?): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val userId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        if (body == null) {
            return@async createBadRequest(MISSING_REQUEST_BODY)
        }
        val imported =
            tocomanController.importProjects(body, userId) ?: return@async createBadRequest("Failed to parse xml")
        createOk(projectTranslator.translate(imported))
    }.asUni()

    override fun exportProject(projectId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val existingProject = projectController.findProject(projectId) ?: return@async createNotFound(
            createNotFoundMessage(
                PROJECT,
                projectId
            )
        )
        val stream = tocomanController.exportProject(existingProject)
        return@async Response.ok(stream)
            .header("Content-Type", "application/xml")
            .build();
    }.asUni()

}