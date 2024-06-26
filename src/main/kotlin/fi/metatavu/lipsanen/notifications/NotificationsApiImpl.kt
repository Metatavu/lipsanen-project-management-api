package fi.metatavu.lipsanen.notifications

import fi.metatavu.lipsanen.api.spec.NotificationsApi
import fi.metatavu.lipsanen.rest.AbstractApi
import fi.metatavu.lipsanen.rest.UserRole
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
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*

/**
 * Notifications API implementation (only for testing purposes)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RequestScoped
class NotificationsApiImpl : NotificationsApi, AbstractApi() {

    @ConfigProperty(name = "environment")
    lateinit var environment: Optional<String>

    @Inject
    lateinit var notificationsController: NotificationsController

    @Inject
    lateinit var vertx: Vertx

    @RolesAllowed(UserRole.ADMIN.NAME)
    @WithTransaction
    override fun deleteNotification(notificationId: UUID): Uni<Response> = CoroutineScope(vertx.dispatcher()).async {
        if (environment.isPresent && environment.get() == "test") {
            return@async createForbidden("Deleting notifications is not allowed out of test environment")
        }
        val notification = notificationsController.find(notificationId) ?: return@async createNotFound("Notification not found")
        notificationsController.delete(notification)
        return@async createNoContent()
    }.asUni()
}