package fi.metatavu.lipsanen.rest

import fi.metatavu.lipsanen.api.model.Error
import fi.metatavu.lipsanen.milestones.MilestoneController
import fi.metatavu.lipsanen.milestones.MilestoneEntity
import fi.metatavu.lipsanen.projects.ProjectController
import fi.metatavu.lipsanen.projects.ProjectEntity
import io.quarkus.security.identity.SecurityIdentity
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.vertx.core.Vertx
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.*
import java.lang.Runnable
import kotlin.coroutines.CoroutineContext
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.*

/**
 * Abstract base class for all API services
 *
 * @author Jari Nykänen
 */
abstract class AbstractApi {

    @Inject
    private lateinit var jsonWebToken: JsonWebToken

    @Inject
    lateinit var identity: SecurityIdentity

    @Inject
    lateinit var projectController: ProjectController

    @Inject
    lateinit var milestoneController: MilestoneController

    /**
     * Checks if user is admin
     *
     * @return if user is admin
     */
    protected fun isAdmin(): Boolean {
        return identity.hasRole(UserRole.ADMIN.NAME)
    }

    /**
     * Checks if user is project owner
     *
     * @return if user is project owner
     */
    protected fun isProjectOwner(): Boolean {
        return identity.hasRole(UserRole.PROJECT_OWNER.NAME)
    }

    /**
     * Returns logged user id
     *
     * @return logged user id
     */
    protected val loggedUserId: UUID?
        get() {
            if (jsonWebToken.subject != null) {
                return UUID.fromString(jsonWebToken.subject)
            }

            return null
        }
    /**
     * Constructs ok response
     *
     * @param entity payload
     * @param count total count
     * @return response
     */
    protected fun createOk(entity: Any?, count: Long): Response {
        return Response
            .status(Response.Status.OK)
            .header("X-Total-Count", count.toString())
            .header("Access-Control-Expose-Headers", "X-Total-Count")
            .entity(entity)
            .build()
    }

    /**
     * Constructs ok response
     *
     * @param entity payload
     * @return response
     */
    protected fun createOk(entity: Any?): Response {
        return Response
            .status(Response.Status.OK)
            .entity(entity)
            .build()
    }

    /**
     * Constructs ok response
     *
     * @return response
     */
    protected fun createOk(): Response {
        return Response
            .status(Response.Status.OK)
            .build()
    }

    /**
     * Constructs no content response
     *
     * @param entity payload
     * @return response
     */
    protected fun createAccepted(entity: Any?): Response {
        return Response
            .status(Response.Status.ACCEPTED)
            .entity(entity)
            .build()
    }

    /**
     * Constructs no content response
     *
     * @return response
     */
    protected fun createNoContent(): Response {
        return Response
            .status(Response.Status.NO_CONTENT)
            .build()
    }

    /**
     * Constructs bad request response
     *
     * @param message message
     * @return response
     */
    protected fun createBadRequest(message: String): Response {
        return createError(Response.Status.BAD_REQUEST, message)
    }

    /**
     * Constructs not found response
     *
     * @param message message
     * @return response
     */
    protected fun createNotFound(message: String): Response {
        return createError(Response.Status.NOT_FOUND, message)
    }

    /**
     * Constructs not found response
     *
     * @return response
     */
    protected fun createNotFound(): Response {
        return Response
            .status(Response.Status.NOT_FOUND)
            .build()
    }
    /**
     * Constructs not found response
     *
     * @param message message
     * @return response
     */
    protected fun createConflict(message: String): Response {
        return createError(Response.Status.CONFLICT, message)
    }

    /**
     * Constructs not implemented response
     *
     * @param message message
     * @return response
     */
    protected fun createNotImplemented(message: String): Response {
        return createError(Response.Status.NOT_IMPLEMENTED, message)
    }

    /**
     * Constructs internal server error response
     *
     * @param message message
     * @return response
     */
    protected fun createInternalServerError(message: String): Response {
        return createError(Response.Status.INTERNAL_SERVER_ERROR, message)
    }

    /**
     * Constructs forbidden response
     *
     * @param message message
     * @return response
     */
    protected fun createForbidden(message: String): Response {
        return createError(Response.Status.FORBIDDEN, message)
    }

    /**
     * Constructs unauthorized response
     *
     * @param message message
     * @return response
     */
    protected fun createUnauthorized(message: String): Response {
        return createError(Response.Status.UNAUTHORIZED, message)
    }

    /**
     * Constructs an error response
     *
     * @param status status code
     * @param message message
     *
     * @return error response
     */
    private fun createError(status: Response.Status, message: String): Response {
        val entity = Error(
            message = message,
            status = status.statusCode
        )

        return Response
            .status(status)
            .entity(entity)
            .build()
    }

    /**
     * Constructs not found message
     *
     * @param entity entity name
     * @param id entity id
     * @return not found message
     */
    fun createNotFoundMessage(entity: String, id: UUID): String {
        return "$entity with id $id not found"
    }

    /**
     * Helper method for getting project or milestone or an error response, also checking user's access rights
     * for the project (ignoring the admin role)
     *
     * @param projectId project id
     * @param milestoneId milestone id
     * @param userId user id
     * @return either data or response with error
     */
    open suspend fun getProjectMilestoneAccessRights(
        projectId: UUID,
        milestoneId: UUID,
        userId: UUID
    ): Pair<Pair<MilestoneEntity, ProjectEntity>?, Response?> {
        val project = projectController.findProject(projectId) ?: return null to createNotFound(
            createNotFoundMessage(
                PROJECT,
                projectId
            )
        )
        val milestone = milestoneController.find(milestoneId)
        if (milestone == null || milestone.project.id != projectId) {
            return null to createNotFound(
                createNotFoundMessage(
                    MILESTONE,
                    milestoneId
                )
            )
        }
        if (!isAdmin() && !projectController.hasAccessToProject(project, userId)) {
            return null to createForbidden(NO_PROJECT_RIGHTS)
        }
        return milestone to project to null
    }

    /**
     * Returns project or error response, checks project access rights (does not apply to global admin)
     *
     * @param projectId project id
     * @param userId user id
     * @return project or error response
     */
    open suspend fun getProjectAccessRights(
        projectId: UUID,
        userId: UUID
    ): Pair<ProjectEntity?, Response?> {
        val project = projectController.findProject(projectId) ?: return null to createNotFound(
            createNotFoundMessage(
                PROJECT,
                projectId
            )
        )

        return getProjectAccessRights(project, userId)
     }

    /**
     * Returns project or error response, checks project access rights (does not apply to global admin)
     *
     * @param project project
     * @param userId user id
     * @return project or error response
     */
    open suspend fun getProjectAccessRights(
        project: ProjectEntity,
        userId: UUID
    ): Pair<ProjectEntity?, Response?> {
        if (!isAdmin() && !projectController.hasAccessToProject(project, userId)) {
            return null to createForbidden(NO_PROJECT_RIGHTS)
        }
        return project to null
    }

    /**
     * Executes a block with coroutine scope
     *
     * @param requestTimeOut request timeout in milliseconds. Default is 10000
     * @param block block to execute
     * @return Uni
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    protected fun <T> withCoroutineScope(requestTimeOut: Long = 10000L, block: suspend () -> T): Uni<T> {
        val context = Vertx.currentContext()
	val dispatcher = VertxCoroutineDispatcher(context)

	return CoroutineScope(context = dispatcher)
	    .async {
		withTimeout(requestTimeOut) {
			block()
		}
	}
	.asUni()
}

/**
 * Custom vertx coroutine dispatcher that keeps the context stable during the execution
 */
private class VertxCoroutineDispatcher(private val vertxContext: io.vertx.core.Context): CoroutineDispatcher() {
	override fun dispatch(context: CoroutineContext, block: Runnable) {
		vertxContext.runOnContext {
			block.run()
		}
	}
}

    companion object {
        const val NOT_FOUND_MESSAGE = "Not found"
        const val UNAUTHORIZED = "Unauthorized"
        const val FORBIDDEN = "Forbidden"
        const val MISSING_REQUEST_BODY = "Missing request body"
        const val INVALID_REQUEST_BODY = "Invalid request body"

        const val PROJECT = "Project"
        const val USER = "User"
        const val COMPANY = "Company"
        const val PROJECT_THEME = "Project theme"
        const val MILESTONE = "Milestone"
        const val TASK = "Task"
        const val TASK_CONNECTION = "Task connection"
        const val CHANGE_PROPOSAL = "Change proposal"
        const val TASK_COMMENT = "Task comment"
        const val JOB_POSITION = "Job position"

        const val NO_PROJECT_RIGHTS = "User does not have access to project"
        const val WRONG_PROJECT_STAGE = "Project is not in planning stage"
        const val INVALID_TASK_DATES = "Task start date cannot be after end date"
        const val INVALID_PROJECT_STATE = "Task updates are only allowed in project planning stage"
    }

}
