package fi.metatavu.lipsanen.rest

import fi.metatavu.lipsanen.api.model.Error
import io.quarkus.security.identity.SecurityIdentity
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
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

    /**
     * Checks if user is admin
     *
     * @return if user is admin
     */
    protected fun isAdmin(): Boolean {
        return identity.hasRole(UserRole.ADMIN.NAME)
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

        const val NO_PROJECT_RIGHTS = "User does not have access to project"
        const val WRONG_PROJECT_STAGE = "Project is not in planning stage"
        const val INVALID_TASK_DATES = "Task start date cannot be after end date"
        const val INVALID_PROJECT_STATE = "Task updates are only allowed in project planning stage"
    }

}
