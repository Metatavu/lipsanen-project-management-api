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

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME)
    override fun listNotificationEvents(
        userId: UUID,
        projectId: UUID?,
        taskId: UUID?,
        first: Int?,
        max: Int?,
        readStatus: Boolean?
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val loggedInUserId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val requestedUser = userController.findUser(userId) ?: return@async createNotFound(createNotFoundMessage(USER, userId))
        if (requestedUser.keycloakId != loggedInUserId && !isAdmin()) {
            return@async createBadRequest("User id does not match logged in user id")
        }

        val receiverFilter = userController.findUser(userId) ?: return@async createNotFound(createNotFoundMessage(USER, userId))

        val projectFilter = if (projectId != null) {
            projectController.findProject(projectId) ?: return@async createNotFound(createNotFoundMessage(PROJECT, projectId))
        } else null

        val taskFilter = if (taskId != null) {
            taskController.find(taskId) ?: return@async createNotFound(createNotFoundMessage(TASK, taskId))
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
    }.asUni()

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME)
    override fun findNotificationEvent(notificationEventId: UUID): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val loggedInUserId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val notificationEvent = notificationEventController.find(notificationEventId)
                ?: return@async createNotFound("Notification event not found")
            if (notificationEvent.receiver.keycloakId != loggedInUserId && !isAdmin()) {
                return@async createForbidden("User does not have access to notification event")
            }

            createOk(notificationEventTranslator.translate(notificationEvent))
        }.asUni()

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME)
    @WithTransaction
    override fun updateNotificationEvent(
        notificationEventId: UUID,
        notificationEvent: NotificationEvent
    ): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        val loggedInUserId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
        val notificationEventEntity = notificationEventController.find(notificationEventId)
            ?: return@async createNotFound("Notification event not found")
        if (notificationEventEntity.receiver.keycloakId != loggedInUserId && !isAdmin()) {
            return@async createForbidden("User does not have access to notification event")
        }

        val updatedNotificationEvent = notificationEventController.updateNotificationEvent(
            existingEntity = notificationEventEntity,
            updateBody = notificationEvent,
            modifierId = loggedInUserId
        )
        createOk(notificationEventTranslator.translate(updatedNotificationEvent))
    }.asUni()

    @RolesAllowed(UserRole.USER.NAME, UserRole.ADMIN.NAME)
    @WithTransaction
    override fun deleteNotificationEvent(notificationEventId: UUID): Uni<Response> =
        CoroutineScope(vertx.dispatcher()).async {
            val loggedInUserId = loggedUserId ?: return@async createUnauthorized(UNAUTHORIZED)
            val notificationEvent = notificationEventController.find(notificationEventId)
                ?: return@async createNotFound("Notification event not found")
            if (notificationEvent.receiver.keycloakId != loggedInUserId && !isAdmin()) {
                return@async createForbidden("User does not have access to notification event")
            }

            notificationEventController.delete(notificationEvent)
            createNoContent()
        }.asUni()
}