package fi.metatavu.lipsanen.notifications.notificationevents

import fi.metatavu.lipsanen.api.model.NotificationEvent
import fi.metatavu.lipsanen.api.spec.NotificationEventsApi
import fi.metatavu.lipsanen.projects.milestones.tasks.TaskController
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
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
import java.util.*

/**
 * Notification events API implementation
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RequestScoped
@WithSession
class NotificationEventsApiImpl : NotificationEventsApi, AbstractApi() {

    @Inject
    lateinit var notificationEventController: NotificationEventsController

    @Inject
    lateinit var notificationEventTranslator: NotificationEventTranslator

    @Inject
    lateinit var taskController: TaskController

    @Inject
    lateinit var userController: UserController

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun listNotificationEvents(
        userId: UUID,
        projectId: UUID?,
        taskId: UUID?,
        first: Int?,
        max: Int?,
        readStatus: Boolean?
    ): Uni<Response> = withCoroutineScope {
        val loggedInUserId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        val requestedUser = userController.findUser(userId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(USER, userId))
        if (requestedUser.keycloakId != loggedInUserId && !isAdmin()) {
            return@withCoroutineScope createBadRequest("User id does not match logged in user id")
        }

        val receiverFilter = userController.findUser(userId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(USER, userId))

        val projectFilter = if (projectId != null) {
            projectController.findProject(projectId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(PROJECT, projectId))
        } else null

        val taskFilter = if (taskId != null) {
            taskController.find(taskId) ?: return@withCoroutineScope createNotFound(createNotFoundMessage(TASK, taskId))
        } else null

        // No access rights checks since the user is the receiver of the notification
        val (notificationEvents, count) = notificationEventController.list(
            receiver = receiverFilter,
            project = projectFilter,
            readStatus = readStatus,
            task = taskFilter,
            first = first,
            max = max
        )

        createOk(notificationEventTranslator.translate(notificationEvents), count)
    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    override fun findNotificationEvent(notificationEventId: UUID): Uni<Response> =
        withCoroutineScope {
            val loggedInUserId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
            val notificationEvent = notificationEventController.find(notificationEventId)
                ?: return@withCoroutineScope createNotFound("Notification event not found")
            if (!isAdmin() && notificationEvent.receiver.keycloakId != loggedInUserId) {
                return@withCoroutineScope createForbidden("User does not have access to notification event")
            }

            createOk(notificationEventTranslator.translate(notificationEvent))
        }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun updateNotificationEvent(
        notificationEventId: UUID,
        notificationEvent: NotificationEvent
    ): Uni<Response> = withCoroutineScope {
        val loggedInUserId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
        val notificationEventEntity = notificationEventController.find(notificationEventId)
            ?: return@withCoroutineScope createNotFound("Notification event not found")
        if (!isAdmin() && notificationEventEntity.receiver.keycloakId != loggedInUserId) {
            return@withCoroutineScope createForbidden("User does not have access to notification event")
        }

        val updatedNotificationEvent = notificationEventController.updateNotificationEvent(
            existingEntity = notificationEventEntity,
            updateBody = notificationEvent,
            modifierId = loggedInUserId
        )
        createOk(notificationEventTranslator.translate(updatedNotificationEvent))
    }

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME, UserRole.PROJECT_OWNER.NAME)
    @WithTransaction
    override fun deleteNotificationEvent(notificationEventId: UUID): Uni<Response> =
        withCoroutineScope {
            val loggedInUserId = loggedUserId ?: return@withCoroutineScope createUnauthorized(UNAUTHORIZED)
            val notificationEvent = notificationEventController.find(notificationEventId)
                ?: return@withCoroutineScope createNotFound("Notification event not found")
            if (!isAdmin() && notificationEvent.receiver.keycloakId != loggedInUserId) {
                return@withCoroutineScope createForbidden("User does not have access to notification event")
            }

            notificationEventController.delete(notificationEvent)
            createNoContent()
        }
}